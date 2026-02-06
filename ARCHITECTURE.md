# java-ac4y-context - Architektúra Dokumentáció

## Áttekintés

Az `ac4y-context` modul context management és factory pattern implementáció az Ac4y projektekhez. Modulspecifikus adatbázis kapcsolatok létrehozását és kezelését biztosítja properties fájlok alapján.

**Verzió:** 1.0.0
**Java verzió:** 11
**Szervezet:** ac4y-auto

## Fő Komponensek

### 1. Context Management

#### `Ac4yContext`
Factory osztály modulspecifikus adatbázis kapcsolatok létrehozásához.

**Felelősség:**
- DBConnection objektumok létrehozása modul alapján
- Properties fájl elnevezési konvenció kezelése
- Modul és osztály alapú konfiguráció támogatása

**Fő metódus:**
```java
public DBConnection getDBConnection(String aModul, String aClassName)
    throws ClassNotFoundException, SQLException, IOException, Ac4yException
```

**Működési Elv:**

1. **Properties fájl név generálása:**
```java
String propertiesFileName = aModul + ".properties";
// Példa: "usermodule" + ".properties" = "usermodule.properties"
```

2. **DBConnection létrehozása:**
```java
return new DBConnection(propertiesFileName);
// Betölti a usermodule.properties fájlt és létrehozza a kapcsolatot
```

**Paraméterek:**
- `aModul` (String): Modul neve, egyben a properties fájl prefix
- `aClassName` (String): Osztály neve (jelenleg nem használt a metódusban, de paraméter a jövőbeli bővíthetőség miatt)

**Kivételek:**
- `ClassNotFoundException`: JDBC driver nem található
- `SQLException`: Adatbázis kapcsolat hiba
- `IOException`: Properties fájl olvasási hiba
- `Ac4yException`: Konfiguráció hiba

## Architektúra Minták

### 1. Factory Pattern
Az `Ac4yContext` factory osztály, amely egységes interfészt biztosít DBConnection objektumok létrehozásához:

```java
Ac4yContext context = new Ac4yContext();
DBConnection conn = context.getDBConnection("mymodule", "MyClass");
```

### 2. Convention over Configuration
A properties fájl neve a modul névből származik automatikusan:
- Modul: "inventory" → Properties: "inventory.properties"
- Modul: "userservice" → Properties: "userservice.properties"

### 3. Separation of Concerns
- **Ac4yContext**: Connection factory logika
- **DBConnection**: Connection létrehozás és properties kezelés
- **Properties fájl**: Konfiguráció tárolása

## Függőségek

### Maven Függőségek

```xml
<dependency>
    <groupId>ac4y</groupId>
    <artifactId>ac4y-base</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>ac4y</groupId>
    <artifactId>ac4y-database</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Tranzitív függőségek:**
- ac4y-utility (1.0.0) - ac4y-base-n keresztül
- ac4y-base teljes funkciókészlete

**Használt osztályok:**
- `ac4y.base.database.DBConnection` - ac4y-database-ből
- `ac4y.base.Ac4yException` - ac4y-base-ből

## Properties Fájl Konvenció

### Elnevezési Szabály

```
{moduleName}.properties
```

**Példák:**
- `inventory.properties`
- `userservice.properties`
- `reporting.properties`
- `authentication.properties`

### Properties Fájl Struktúra

Minden modul properties fájlnak tartalmaznia kell az alábbi kulcsokat:

```properties
# JDBC Driver
driver=com.mysql.cj.jdbc.Driver

# Connection String
connectionString=jdbc:mysql://localhost:3306/{moduleName}_db?useSSL=false&serverTimezone=UTC

# Database Credentials
dbuser=dbusername
dbpassword=dbpassword
```

### Példa Properties Fájlok

#### inventory.properties
```properties
driver=com.mysql.cj.jdbc.Driver
connectionString=jdbc:mysql://localhost:3306/inventory_db?useSSL=false
dbuser=inventory_user
dbpassword=inv_pass_123
```

#### userservice.properties
```properties
driver=org.postgresql.Driver
connectionString=jdbc:postgresql://localhost:5432/users_db
dbuser=user_admin
dbpassword=user_pass_456
```

#### reporting.properties
```properties
driver=oracle.jdbc.driver.OracleDriver
connectionString=jdbc:oracle:thin:@localhost:1521:reportdb
dbuser=report_user
dbpassword=report_pass_789
```

## Tipikus Használati Minták

### 1. Egyszerű Modul Connection

```java
try {
    Ac4yContext context = new Ac4yContext();

    // inventory.properties alapján connection
    DBConnection dbConnection = context.getDBConnection("inventory", "InventoryService");
    Connection conn = dbConnection.getConnection();

    // Query végrehajtása
    PreparedStatement ps = conn.prepareStatement("SELECT * FROM products");
    ResultSet rs = ps.executeQuery();

    while (rs.next()) {
        // feldolgozás
    }

    rs.close();
    ps.close();
    conn.close();

} catch (ClassNotFoundException | SQLException | IOException | Ac4yException e) {
    ErrorHandler.addStack(e);
}
```

### 2. Multi-Module Alkalmazás

```java
public class MultiModuleApp {

    private Ac4yContext context = new Ac4yContext();

    public void processInventory() throws Exception {
        DBConnection inventoryDB = context.getDBConnection("inventory", getClass().getName());
        Connection conn = inventoryDB.getConnection();
        // inventory műveletek
        conn.close();
    }

    public void processUsers() throws Exception {
        DBConnection userDB = context.getDBConnection("userservice", getClass().getName());
        Connection conn = userDB.getConnection();
        // user műveletek
        conn.close();
    }

    public void processReports() throws Exception {
        DBConnection reportDB = context.getDBConnection("reporting", getClass().getName());
        Connection conn = reportDB.getConnection();
        // reporting műveletek
        conn.close();
    }
}
```

### 3. Service Layer Pattern

```java
public class InventoryService {

    private static final String MODULE_NAME = "inventory";
    private Ac4yContext context;

    public InventoryService() {
        this.context = new Ac4yContext();
    }

    public List<Product> findAllProducts() throws Exception {
        DBConnection dbConnection = context.getDBConnection(MODULE_NAME, getClass().getName());
        Connection conn = dbConnection.getConnection();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM products");
             ResultSet rs = ps.executeQuery()) {

            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(mapProduct(rs));
            }
            return products;

        } finally {
            conn.close();
        }
    }

    public void updateStock(int productId, int quantity) throws Exception {
        DBConnection dbConnection = context.getDBConnection(MODULE_NAME, getClass().getName());
        Connection conn = dbConnection.getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement ps = conn.prepareStatement(
                "UPDATE products SET stock = stock + ? WHERE id = ?"
            );
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            ps.executeUpdate();

            conn.commit();
            ps.close();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getInt("stock")
        );
    }
}
```

### 4. DAO Pattern Integration

```java
public abstract class BaseDAO {

    protected Ac4yContext context = new Ac4yContext();
    protected abstract String getModuleName();

    protected Connection getConnection() throws Exception {
        DBConnection dbConnection = context.getDBConnection(
            getModuleName(),
            getClass().getName()
        );
        return dbConnection.getConnection();
    }
}

public class UserDAO extends BaseDAO {

    @Override
    protected String getModuleName() {
        return "userservice";
    }

    public User findById(int id) throws Exception {
        Connection conn = getConnection();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"));
            }
            return null;
        } finally {
            conn.close();
        }
    }
}

public class ProductDAO extends BaseDAO {

    @Override
    protected String getModuleName() {
        return "inventory";
    }

    public List<Product> findAll() throws Exception {
        Connection conn = getConnection();

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM products");
             ResultSet rs = ps.executeQuery()) {

            List<Product> products = new ArrayList<>();
            while (rs.next()) {
                products.add(new Product(rs.getInt("id"), rs.getString("name")));
            }
            return products;
        } finally {
            conn.close();
        }
    }
}
```

### 5. Process Integration

```java
public class InventoryProcess extends Ac4yProcess {

    private Ac4yContext context = new Ac4yContext();

    @Override
    public Object process(Object input) throws Ac4yException {
        try {
            DBConnection dbConnection = context.getDBConnection("inventory", getClass().getName());
            Connection conn = dbConnection.getConnection();

            // Process logic with database
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE active = ?");
            ps.setBoolean(1, true);
            ResultSet rs = ps.executeQuery();

            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                results.add(row);
            }

            rs.close();
            ps.close();
            conn.close();

            return results;

        } catch (Exception e) {
            throw new Ac4yException("Process failed: " + e.getMessage());
        }
    }
}
```

### 6. Configuration Management

```java
public class ConfigurationManager {

    private Ac4yContext context = new Ac4yContext();
    private Map<String, DBConnection> connectionCache = new HashMap<>();

    public synchronized DBConnection getModuleConnection(String moduleName)
            throws Exception {

        if (!connectionCache.containsKey(moduleName)) {
            DBConnection conn = context.getDBConnection(moduleName, getClass().getName());
            connectionCache.put(moduleName, conn);
        }

        return connectionCache.get(moduleName);
    }

    public void closeAllConnections() {
        for (DBConnection dbConn : connectionCache.values()) {
            try {
                dbConn.getConnection().close();
            } catch (SQLException e) {
                ErrorHandler.addStack(e);
            }
        }
        connectionCache.clear();
    }
}
```

## Modul Izolációs Stratégiák

### 1. Adatbázis per Modul

Minden modul saját adatbázist használ:

```properties
# inventory.properties
connectionString=jdbc:mysql://localhost:3306/inventory_db

# userservice.properties
connectionString=jdbc:mysql://localhost:3306/user_db

# reporting.properties
connectionString=jdbc:mysql://localhost:3306/report_db
```

**Előnyök:**
- Teljes adatizoláció
- Független skálázhatóság
- Különböző JDBC driver-ek használhatók

### 2. Schema per Modul

Egy adatbázis, több schema:

```properties
# inventory.properties
connectionString=jdbc:postgresql://localhost:5432/myapp?currentSchema=inventory

# userservice.properties
connectionString=jdbc:postgresql://localhost:5432/myapp?currentSchema=users

# reporting.properties
connectionString=jdbc:postgresql://localhost:5432/myapp?currentSchema=reports
```

**Előnyök:**
- Egyszerűbb management
- Cross-schema query lehetőség
- Közös backup

### 3. Prefix per Modul

Egy adatbázis, prefix alapú táblanevek:

```properties
# Minden modul ugyanazt a DB-t használja
# inventory.properties
connectionString=jdbc:mysql://localhost:3306/myapp_db
# Táblák: inv_products, inv_stock, inv_orders

# userservice.properties
connectionString=jdbc:mysql://localhost:3306/myapp_db
# Táblák: user_accounts, user_profiles, user_sessions
```

**Előnyök:**
- Legegyszerűbb konfiguráció
- Egy connection pool elég
- Kis alkalmazásokhoz ideális

## AI Agent Használati Útmutató

### Gyors Döntési Fa

**Kérdés:** Mit szeretnél elérni?

1. **Modulspecifikus DB kapcsolat** → `Ac4yContext.getDBConnection(moduleName, className)`
   - Több modul, több DB? → Context factory használata kötelező
   - Properties fájl {modul}.properties formátumban

2. **Egy modul, egy DB** → `ac4y-database` elég
   - Csak egy adatbázis van? → DBConnection direkt használata egyszerűbb

3. **Java EE környezet** → `ac4y-connection-pool`
   - Application server-ben fut? → JNDI pool ajánlott

### Token-hatékony Tudás

**Mit tartalmaz:**
- Factory pattern modulokhoz
- Convention-based properties loading
- Multi-database support

**Mit NEM tartalmaz:**
- Connection pool implementáció
- Connection caching
- Transaction management

**Függőségek:**
- ac4y-base (1.0.0)
- ac4y-database (1.0.0)

**Konvenció:**
- Properties fájl: `{moduleName}.properties`
- Classpath-ról töltődik be
- Standard DBConnection format

**Kivételek:**
- ClassNotFoundException, SQLException, IOException, Ac4yException

**Használat:**
```java
Ac4yContext ctx = new Ac4yContext();
DBConnection db = ctx.getDBConnection("module", "Class");
Connection conn = db.getConnection();
```

## Összehasonlítás más Ac4y Modulokkal

| Modul | Használati Eset | Connection Típus | Konfiguráció |
|-------|----------------|------------------|--------------|
| **ac4y-database** | Egy modul, egy DB | Direct JDBC | Properties fájl |
| **ac4y-connection-pool** | Java EE, pooling | JNDI DataSource | Application server |
| **ac4y-context** | Több modul, több DB | Direct JDBC, factory | Modul-specifikus properties |

### Mikor használd az ac4y-context-et?

**IGEN, használd:**
- ✅ Multi-module alkalmazás
- ✅ Modulonként külön adatbázis
- ✅ Service-oriented architecture
- ✅ Microservices-szerű monolit

**NEM, ne használd:**
- ❌ Egy modul, egy adatbázis (→ ac4y-database)
- ❌ Java EE pooling kell (→ ac4y-connection-pool)
- ❌ Spring Boot (→ DataSource auto-configuration)

## Bővítési Lehetőségek

### 1. Connection Caching

```java
public class CachedAc4yContext extends Ac4yContext {

    private Map<String, DBConnection> cache = new ConcurrentHashMap<>();

    @Override
    public DBConnection getDBConnection(String aModul, String aClassName) throws Exception {
        return cache.computeIfAbsent(aModul, k -> {
            try {
                return super.getDBConnection(k, aClassName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

### 2. Environment-specific Configuration

```java
public class EnvironmentAwareContext extends Ac4yContext {

    private String environment = System.getProperty("app.env", "dev");

    @Override
    public DBConnection getDBConnection(String aModul, String aClassName) throws Exception {
        String propertiesFile = aModul + "." + environment + ".properties";
        return new DBConnection(propertiesFile);
        // Példa: inventory.dev.properties, inventory.prod.properties
    }
}
```

### 3. Monitoring Wrapper

```java
public class MonitoredContext extends Ac4yContext {

    @Override
    public DBConnection getDBConnection(String aModul, String aClassName) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            DBConnection conn = super.getDBConnection(aModul, aClassName);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Connection to " + aModul + " took " + duration + "ms");
            return conn;
        } catch (Exception e) {
            System.err.println("Failed to connect to " + aModul + ": " + e.getMessage());
            throw e;
        }
    }
}
```

## Troubleshooting

### Problem: Properties fájl nem található

**Hiba:**
```
IOException: inventory.properties not found
```

**Megoldás:**
1. Ellenőrizd, hogy a properties fájl a classpath-on van
2. Maven projektben: `src/main/resources/inventory.properties`
3. Ellenőrizd a fájlnév elírást

### Problem: Rossz modul név

**Hiba:**
```
IOException: myModul.properties not found
```

**Megoldás:**
- A modul név case-sensitive
- Ellenőrizd: `getDBConnection("myModul", ...)` vs `mymodule.properties`

### Problem: ClassNotFoundException

**Hiba:**
```
ClassNotFoundException: com.mysql.cj.jdbc.Driver
```

**Megoldás:**
- JDBC driver dependency hiányzik a pom.xml-ből
- Add hozzá a megfelelő JDBC driver-t

## Build és Telepítés

```bash
# Build
mvn clean install

# Test
mvn test

# Deploy to GitHub Packages
mvn deploy
```

**GitHub Packages:**
```xml
<dependency>
    <groupId>ac4y</groupId>
    <artifactId>ac4y-context</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Best Practices

1. **Konvenció követése**: Properties fájlok neve = modulnév
2. **Classpath**: Properties fájlok src/main/resources/-ban
3. **Naming**: Modulnevek legyenek lowercase, beszédesek
4. **Separation**: Egy modul = egy felelősségi kör = egy DB
5. **Documentation**: Dokumentáld, melyik modul melyik DB-t használja
6. **Environment**: Használj különböző properties fájlokat dev/test/prod-ra
7. **Error handling**: Mindig kezeld a kivételeket
8. **Connection closing**: Ne felejts el close()-olni

## Összefoglalás

Az `ac4y-context` modul ideális multi-module alkalmazásokhoz, ahol:
- Több független modul van
- Modulonként saját adatbázis kell
- Convention-based konfiguráció előnyös
- Factory pattern egyszerűsíti a connection kezelést

Egyszerű, könnyen használható, és jól skálázható arhitektúrát biztosít moduláris Java alkalmazásokhoz.
