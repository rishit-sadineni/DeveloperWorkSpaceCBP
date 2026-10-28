# Developer Workspace — Java + DBMS CBP

A complete desktop Developer Workspace built with:

- Java 17
- Swing
- JDBC
- MySQL
- Maven

## Features

1. Login
2. Dashboard
3. Project Manager
4. Task / Issue Tracker
5. Code Snippet Library
6. DSA Tracker
7. Bookmark Manager
8. Coding Timer
9. Statistics

## Database tables

- users
- projects
- tasks
- snippets
- dsa_topics
- dsa_problems
- bookmarks
- coding_sessions

The project uses foreign keys, primary keys, constraints, joins, aggregate queries, GROUP BY, LIKE searching, transactions and prepared statements.

## 1. Install prerequisites

Install:

- JDK 17+
- MySQL Server 8+
- Maven 3.8+

Check:

```bash
java -version
mvn -version
mysql --version
```

## 2. Configure MySQL credentials

Open:

`src/main/java/com/developerworkspace/DBConfig.java`

Change:

```java
public static final String USER = "root";
public static final String PASSWORD = "YOUR_MYSQL_PASSWORD";
```

You can also use environment variables:

```text
DW_DB_USER
DW_DB_PASSWORD
DW_DB_HOST
DW_DB_PORT
```

## 3. Create the database

The application automatically creates the database `developer_workspace` and all required tables on startup.

You do NOT need to manually create every table.

If you prefer SQL manually, run:

`src/main/resources/schema.sql`

## 4. Run

From the project directory:

```bash
mvn clean compile
mvn exec:java
```

Maven automatically downloads MySQL Connector/J.

## 5. Login

The first run creates:

```text
Username: admin
Password: admin123
```

Change the password later if you extend the project.

## 6. How Java connects to MySQL

The connection is handled by:

`DBConnection.java`

The important JDBC sequence is:

1. Load/connect using JDBC
2. Create `Connection`
3. Create `PreparedStatement`
4. Execute SQL
5. Read `ResultSet`
6. Close resources

Example:

```java
try (Connection con = DBConnection.getConnection();
     PreparedStatement ps = con.prepareStatement(
         "SELECT * FROM projects WHERE user_id = ?")) {

    ps.setInt(1, userId);

    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            System.out.println(rs.getString("name"));
        }
    }
}
```

The project uses PreparedStatement rather than string-concatenated SQL. This is important for the SQL Injection topic in your syllabus.

## 7. Project structure

```text
DeveloperWorkspaceCBP
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/com/developerworkspace
    │   │   ├── Main.java
    │   │   ├── DBConfig.java
    │   │   ├── DBConnection.java
    │   │   ├── DatabaseInitializer.java
    │   │   ├── AppException.java
    │   │   ├── CrudRepository.java
    │   │   ├── BasePanel.java
    │   │   ├── LoginFrame.java
    │   │   ├── MainFrame.java
    │   │   ├── DashboardPanel.java
    │   │   ├── ProjectsPanel.java
    │   │   ├── TasksPanel.java
    │   │   ├── SnippetsPanel.java
    │   │   ├── DSAPanel.java
    │   │   ├── BookmarksPanel.java
    │   │   ├── TimerPanel.java
    │   │   ├── StatisticsPanel.java
    │   │   └── PasswordUtil.java
    │   └── resources
    │       └── schema.sql
```

## 8. Where syllabus concepts appear

### OOP
- Classes and objects
- Constructors
- Encapsulation
- Access control
- `this`
- Method overloading
- Inheritance
- Method overriding
- Abstract class
- Interface
- Generics
- Polymorphism

### Exception Handling
- try/catch
- finally / try-with-resources
- custom exception `AppException`
- SQL exception handling

### Multithreading
- Coding timer uses a Swing `Timer`
- Statistics/dashboard use separate database operations without blocking the UI where practical

### Collections
- `List`
- `Map`
- `Set`

### Event Handling
- JButton action listeners
- JComboBox listeners
- Mouse events
- Swing delegation event model

### Swing
- JFrame
- JPanel
- JLabel
- JTextField
- JPasswordField
- JButton
- JTable
- JComboBox
- JCheckBox
- JMenuBar
- CardLayout
- BorderLayout
- GridLayout
- JScrollPane

### JDBC
- Connection
- PreparedStatement
- Statement
- ResultSet
- transactions
- generated keys

### DBMS
- Primary keys
- Foreign keys
- NOT NULL
- UNIQUE
- CHECK
- relationships
- joins
- aggregation
- normalization-friendly schema
- indexes

## 9. Important

Do not open individual `.java` files and run them randomly.

Run the whole Maven project:

```bash
mvn clean compile
mvn exec:java
```

If MySQL is not running, start MySQL first.

If you get:

`Communications link failure`

MySQL is probably not running or the host/port is wrong.

If you get:

`Access denied for user`

Your username/password in `DBConfig.java` is wrong.

