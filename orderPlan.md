# План: класс заказа и работа с локальной БД (SQLite)

## Контекст
Проект: консольный симулятор службы доставки (Практическая работа 1).
Уже реализованы сущности людей и доставки:
- `people/Person` (абстрактный), `people/Client`, `people/Courier`, `people/Transport`
- `delivery_model/Delivery` (интерфейс) + `StandardDelivery`, `ExpressDelivery`, `FastDelivery`, `PickUpDelivery`
- кастомные исключения в `MyException/`

Чего не хватает по ТЗ: **заказ**, **позиция заказа**, **статусы**, и **хранение заказов**.
Дополнительно по просьбе команды делаем реальную локальную БД (SQLite), а не только память.

## Решения (согласованы)
1. **БД:** SQLite, файл `delivery.db` создаётся автоматически в корне проекта.
   Драйвер: `org.xerial:sqlite-jdbc`.
2. **Сборка:** Maven (`pom.xml`). В `pom.xml` прописываем
   `<sourceDirectory>src</sourceDirectory>`, чтобы **не двигать** текущую папку `src`.
3. **Архитектура:** `Order` — чистая сущность с бизнес-проверками;
   работа с БД — в отдельных repository-классах (DAO). Так требует ТЗ
   о разделении ответственности (не God-Object).
4. **Статусы:** `CREATED -> ASSIGNED -> IN_DELIVERY -> DELIVERED`, плюс `CANCELLED`.
5. **Клиент и курьер:** отдельные таблицы (`clients`, `couriers`) + внешние ключи.

## Схема БД (файл delivery.db)
```
clients(client_id PK, name, surname, age, phone_number, email, address)
couriers(courier_id PK, name, surname, age, phone_number, transport, is_available)
orders(order_number PK, client_id FK -> clients, address, delivery_type,
       courier_id FK -> couriers, status, total_price)
order_items(id PK AUTOINCREMENT, order_number FK -> orders,
            product_name, quantity, price)
```
- `delivery_type` хранится строкой (имя способа доставки).
- `courier_id` может быть NULL (например, при самовывозе).
- При первом запуске repository сам делает `CREATE TABLE IF NOT EXISTS`.

## Новые файлы
### `pom.xml` (корень)
- Maven-проект, Java 17+.
- Зависимость `org.xerial:sqlite-jdbc`.
- `<sourceDirectory>src</sourceDirectory>` — существующие файлы остаются на месте.

### `order_model/OrderItem.java`
Позиция заказа: `productName`, `quantity`, `price`.
Метод `getTotal() = quantity * price`. Проверка: количество > 0, цена >= 0.

### `order_model/OrderStatus.java`
enum: `CREATED, ASSIGNED, IN_DELIVERY, DELIVERED, CANCELLED`.
Метод `canTransitionTo(OrderStatus next)` — карта допустимых переходов:
- CREATED -> ASSIGNED, CANCELLED
- ASSIGNED -> IN_DELIVERY, CANCELLED
- IN_DELIVERY -> DELIVERED
- DELIVERED, CANCELLED -> (терминальные)

### `order_model/DeliveryFactory.java`
Восстанавливает объект `Delivery` по строке из БД
(`"StandardDelivery"`, `"ExpressDelivery"` и т.д.), чтобы repository
мог собрать заказ из таблицы.

### `order_model/Order.java` (сущность)
Поля: `orderNumber`, `Client client`, `String address`,
`List<OrderItem> items`, `Delivery delivery`, `Courier courier`,
`OrderStatus status`, `float totalPrice`.

Методы с проверками (инкапсуляция по ТЗ):
- `addItem(OrderItem)` — только в статусе CREATED, пересчитывает сумму;
- `setDelivery(Delivery)` — только в CREATED, пересчитывает сумму;
- `assignCourier(Courier)` — только если `delivery.isNeedCourier()==true`
  и `courier.isAvailable()==true`; после назначения вызывает `courier.takeOrder()`;
- `changeStatus(OrderStatus)` — только через `OrderStatus.canTransitionTo(...)`;
- `calcTotal()` — сумма позиций + `delivery.calcPrice()`.

### `order_model/ClientRepository.java` и `order_model/CourierRepository.java`
CRUD для клиентов и курьеров (JDBC):
`insert`, `findById`, `findAll`, `update`, `delete`.
Нужны, чтобы `orders` мог ссылаться на клиента/курьера по id.

### `order_model/OrderRepository.java`
Вся работа с таблицами `orders` и `order_items`:
- `insert(Order)` — транзакция: вставка заказа + всех позиций;
- `findByNumber(int)`, `findAll()`, `update(Order)`, `delete(int)`;
- соединение через `DriverManager.getConnection("jdbc:sqlite:delivery.db")`;
- инициализация схемы (`CREATE TABLE IF NOT EXISTS`) в конструкторе/static-блоке.

## Бизнес-правила
- Стоимость заказа = сумма позиций + `delivery.calcPrice()`.
- Курьер назначается только если способ доставки этого требует и курьер свободен.
- Статус меняется только по допустимым переходам.
- Позиции/доставку нельзя менять после выхода из CREATED.

## Порядок реализации
1. `pom.xml`, проверить, что драйвер SQLite подтянулся, и что `src` компилируется Maven'ом.
2. `OrderItem`, `OrderStatus`, `DeliveryFactory`.
3. `ClientRepository`, `CourierRepository`.
4. `Order` (сущность) + `OrderRepository` (БД).
5. Позже: `service/OrderService.java` и `ui/ConsoleUI.java` — консольное меню
   и разделение ввода/логики/вывода, как требует ТЗ.

## Что НЕ трогаем
- Никакие существующие файлы в `people/`, `delivery_model/`, `MyException/` не редактируются.
- Существующая валидация в `Client`/`Courier`/`Person` переиспользуется как есть.

## Открытые вопросы (уточнить при реализации)
- Номер заказа: генерировать через `AUTOINCREMENT` или вручную (`max+1`)?
- Нужно ли кэшировать объекты `Client`/`Courier` или каждый раз читать из БД?
