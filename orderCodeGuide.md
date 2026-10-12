# Guide по коду: слой заказов и работа с БД

Короткий разбор «что за что отвечает», чтобы можно было объяснить код на защите.
Сначала — общая картина, потом каждый класс и его методы.

## Общая идея (архитектура)

Разделение ответственности:

- **Сущности** (`people`, `delivery_model`, `order_model/Order*`) — только данные и бизнес-правила, про БД ничего не знают.
- **Слой доступа к данным / репозитории** (`order_model/*Repository`, `DatabaseManager`) — только SQL и перевод строк БД в объекты.
- **Точка входа** (позже) — `OrderService` и `ConsoleUI`: меню, ввод/вывод, вызовы репозиториев.

Поток данных: `ConsoleUI -> OrderService -> OrderRepository -> DatabaseManager -> SQLite`.

Принципы, которые тут видны:
- **Инкапсуляция**: поля `Order` приватные, меняются только через методы с проверками.
- **Композиция**: `Order` содержит `Client`, список `OrderItem` и `Delivery`.
- **Полиморфизм**: `Delivery` — интерфейс, стоимость/срок считает конкретная реализация, без `if/switch` по типу.
- **DAO**: работа с БД вынесена в отдельные репозитории.

## Схема БД (SQLite, файл `delivery.db`)

```
clients(client_id PK, name, surname, age, phone_number, email, address)
couriers(courier_id PK, name, surname, age, phone_number, transport, is_available)
orders(order_number PK AUTOINCREMENT, client_id FK->clients, address,
       delivery_type, courier_id FK->couriers, status, total_price)
order_items(id PK AUTOINCREMENT, order_number FK->orders,
            product_name, quantity, price)
```

- `delivery_type` — короткое имя класса, например `ExpressDelivery`.
- `courier_id` может быть `NULL` (самовывоз).
- `is_available`: 1 — свободен, 0 — занят.

---

## `MyException/DatabaseException.java`

Пользовательское **unchecked**-исключение (`extends RuntimeException`) для ошибок БД.

- `DatabaseException(String message)` — сообщение.
- `DatabaseException(String message, Throwable cause)` — сообщение + причина.

Зачем: репозитории не «светят» наружу `SQLException` (checked), а заворачивают его сюда. Плюс сюда же оборачиваются ошибки валидации, если в БД лежат «битые» данные.

---

## `order_model/DatabaseManager.java`

Отвечает за **подключение к SQLite и создание схемы**.

Поля:
- `url` — JDBC-URL базы (например `jdbc:sqlite:delivery.db`).
- `SCHEMA` — массив `CREATE TABLE IF NOT EXISTS ...`.

Методы:
- `DatabaseManager(String url)` — конструктор; сохраняет URL и вызывает `initializeSchema()`.
- `Connection getConnection()` — открывает соединение и выполняет `PRAGMA foreign_keys = ON` (в SQLite внешние ключи по умолчанию выключены и включаются на каждое соединение).
- `initializeSchema()` (private) — проходит по `SCHEMA` и создаёт таблицы. Ошибки заворачивает в `DatabaseException`.

Зачем: единая точка подключения, репозитории не дублируют код соединения.

---

## `order_model/OrderItem.java`

**Сущность «позиция заказа»** — строки товаров. Неизменяемый (immutable): поля `final`.

Поля: `productName`, `quantity`, `price`.

Методы:
- `OrderItem(String productName, int quantity, float price)` — конструктор с валидацией: имя не пустое, количество > 0, цена >= 0. Иначе `IllegalArgumentException`.
- `getProductName()`, `getQuantity()`, `getPrice()` — геттеры.
- `getTotal()` — стоимость позиции: `quantity * price`.
- `toString()` — читаемое представление.
- `equals()` / `hashCode()` — сравнение по значению (по всем трём полям).

---

## `order_model/OrderStatus.java`

**enum статусов заказа** и правила переходов.

Константы: `CREATED`, `ASSIGNED`, `IN_DELIVERY`, `DELIVERED`, `CANCELLED`.

Поля:
- `ALLOWED_TRANSITIONS` — статическая `EnumMap<OrderStatus, Set<OrderStatus>>` с допустимыми переходами. Заполняется в `static`-блоке (нельзя использовать `EnumSet` в инициализаторе самой константы — поэтому `EnumMap` в статике).

Разрешённые переходы:
- `CREATED -> ASSIGNED, IN_DELIVERY, CANCELLED`
- `ASSIGNED -> IN_DELIVERY, CANCELLED`
- `IN_DELIVERY -> DELIVERED, CANCELLED`
- `DELIVERED`, `CANCELLED` — конечные.

Методы:
- `canTransitionTo(OrderStatus next)` — можно ли перейти из текущего в `next`.
- `isFinal()` — статус конечный (`DELIVERED` или `CANCELLED`).

---

## `order_model/DeliveryFactory.java`

**Фабрика** способов доставки — превращает строку из БД в объект `Delivery`.

- Приватный конструктор — класс утилитный, экземпляры не нужны.
- `create(String type)` — по короткому имени класса (`"ExpressDelivery"`) или русскому названию (`"Экспресс-доставка"`) возвращает нужную реализацию `Delivery`. Неизвестный тип -> `IllegalArgumentException`. Используется `switch`-выражение.
- `typeName(Delivery delivery)` — обратное преобразование: даёт короткое имя класса для сохранения в БД (или `null`).

Зачем: реализации `Delivery` не имеют состояния, поэтому фабрика просто создаёт новый объект нужного типа.

---

## `order_model/Order.java`

**Сущность «заказ»** — сердце бизнес-логики. Всё состояние инкапсулировано и меняется через методы с проверками.

Поля:
- `orderNumber` — номер (присваивает БД при `save`, до этого 0).
- `client` — клиент (`final`, не меняется).
- `address` — адрес доставки.
- `items` — список позиций (`final`, наполняется через `addItem`).
- `delivery` — выбранный способ доставки.
- `courier` — назначенный курьер.
- `status` — текущий статус.
- `totalPrice` — итоговая стоимость.

Конструкторы:
- `Order(Client client, String address)` — **новый заказ**: статус `CREATED`, сумма 0. Клиент обязателен, адрес не пустой.
- `Order(...)` (package-private) — **восстановление из БД**: принимает номер, доставку, курьера, статус и позиции. Используется только `OrderRepository` (в том же пакете).

Геттеры: `getOrderNumber`, `getClient`, `getAddress`, `getItems` (возвращает **неизменяемый** список), `getDelivery`, `getCourier`, `getStatus`, `getTotalPrice`.

Методы-изменения:
- `setOrderNumber(int)` (package-private) — проставляет номер после вставки в БД.
- `setAddress(String)` — валидирует, что адрес не пустой.
- `addItem(OrderItem)` — добавляет позицию. Только пока статус `CREATED`, иначе `IllegalStateException`. После — пересчёт суммы.
- `setDelivery(Delivery)` — задаёт способ доставки. Только в `CREATED`. Пересчёт суммы.
- `assignCourier(Courier)` — назначает курьера:
  1. заказ не завершён;
  2. выбран способ доставки;
  3. этот способ требует курьера (`delivery.isNeedCourier()`);
  4. курьер свободен (`courier.isAvailable()`).
  При успехе вызывает `courier.takeOrder()` и переводит заказ в `ASSIGNED`.
- `changeStatus(OrderStatus next)` — меняет статус: проверяет `canTransitionTo(...)`, затем `requireCourierFor(...)` (для `ASSIGNED` и для `IN_DELIVERY` у курьерской доставки курьер обязателен). При завершении — освобождает курьера.
- `recalculateTotal()` — `сумма позиций + delivery.calcPrice()`.

Приватные помощники:
- `requireMutable()` — запрещает менять состав/доставку, если статус не `CREATED`.
- `requireCourierFor(next)` — проверяет наличие курьера для целевого статуса.
- `releaseCourierIfFinished()` — при конечном статусе вызывает `courier.release()`.

Связь с существующим кодом: использует `delivery_model.Delivery` (полиморфизм), `people.Client`, `people.Courier` (в т.ч. `takeOrder()`/`release()`/`isAvailable()`).

---

## `order_model/ClientRepository.java`

**DAO для таблицы `clients`.** Инкапсулирует SQL, наружу отдаёт `Client`.

Поля: `COLUMNS` (список колонок), `databaseManager`.

Методы:
- `ClientRepository(DatabaseManager)` — внедрение зависимости.
- `save(Client)` — `INSERT` нового клиента.
- `findById(int)` — `SELECT ... WHERE client_id = ?` -> `Optional<Client>`.
- `findAll()` — все клиенты, отсортированы по id.
- `update(Client)` — `UPDATE` по `client_id`.
- `delete(int)` — `DELETE` по `client_id`.
- `fillStatement(ps, client)` (private) — раскладка полей объекта по `?`.
- `map(ResultSet)` (private) — сборка `Client` из строки. Ошибки валидации конструктора заворачивает в `DatabaseException`.

---

## `order_model/CourierRepository.java`

**DAO для таблицы `couriers`.** Аналогичен `ClientRepository`.

Отличие в `map()`: 
- `transport` восстанавливается через `Transport.valueOf(...)`;
- доступность восстанавливается не прямым полем, а методами домена: если `is_available = 0`, вызывается `courier.takeOrder()` (это и делает курьера занятым).

Методы: `save`, `findById`, `findAll`, `update`, `delete`, `fillStatement` (private), `map` (private).

---

## `order_model/OrderRepository.java`

**DAO для `orders` + `order_items`** — самый важный класс слоя БД.

Поля: `COLUMNS`, `databaseManager`, `clientRepository`, `courierRepository`.

Конструктор: `OrderRepository(DatabaseManager, ClientRepository, CourierRepository)` — связанные сущности читаются через свои репозитории.

Методы:
- `save(Order)` — в **одной транзакции**: `INSERT` заказа с `RETURN_GENERATED_KEYS`, затем `INSERT` всех позиций (`addBatch`/`executeBatch`). Номер от БД проставляется в объект. При ошибке — `rollback()`.
- `findByNumber(int)` — читает заказ по номеру, подгружает позиции, клиента и курьера -> `Optional<Order>`.
- `findAll()` — все заказы. Чтобы не делать N+1 запросов: читает все строки, все позиции одним запросом и группирует, а клиентов/курьеров кладёт в `Map` по id.
- `update(Order)` — в транзакции: `UPDATE` заказа + удаление старых позиций + вставка новых.
- `delete(int)` — в транзакции удаляет позиции, затем сам заказ.

Приватные помощники:
- `fillOrderStatement` — раскладка полей заказа по `?` (курьер может быть `NULL`).
- `readGeneratedKey` — достаёт сгенерированный `order_number`.
- `readRow` — плоская запись строки заказа (`record OrderRow`).
- `loadItems` / `loadAllItems` / `mapItem` — чтение позиций.
- `buildOrder(...)` (3 перегрузки) — сборка `Order` из строки + связанных объектов.
- `indexClients()` / `indexCouriers()` — `Map<id, объект>` для быстрого поиска.
- `record OrderRow(...)` — вспомогательный носитель одной строки `orders`.

---

## Сборка и запуск

```bash
javac -encoding UTF-8 -d out -cp lib/sqlite-jdbc.jar $(find src -name '*.java')
java -cp "out:lib/sqlite-jdbc.jar" <ГлавныйКласс>
```

`delivery.db` создаётся автоматически при первом запуске.

## Что ещё в `orderPlan.md`

Там исходный план с обоснованием решений, схемой БД и порядком работ. Этот файл — про то, **что уже написано и как это объяснять**.
