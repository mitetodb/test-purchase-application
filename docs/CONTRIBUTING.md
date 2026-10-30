# Contributing to Test Purchase Application

Thank you for your interest in contributing! This guide will help you get started.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- Docker and Docker Compose (for local development)
- Git
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

### Setting Up Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/mitetodb/test-purchase-application.git
   cd test-purchase-application
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your local settings
   nano .env
   ```

3. **Start dependencies with Docker**
   ```bash
   docker compose up -d
   # This starts: MySQL, pricing microservice, and the app
   ```

4. **Access the application**
   - Application: http://localhost:8080
   - MySQL: localhost:3307 (from host)
   - Pricing service: http://localhost:8081 (internal)

5. **Run tests**
   ```bash
   mvn test
   ```

### Running Without Docker

If you prefer to run the application directly:

```bash
# Set environment variables
export DB_URL=jdbc:mysql://localhost:3306/test_purchase_db
export DB_USERNAME=root
export DB_PASSWORD=your_password
export ADMIN_PASSWORD=admin123
export PRICING_API_KEY=your_api_key

# Run the application
mvn spring-boot:run
```

## Development Workflow

### Before You Start

1. **Check existing issues**: Look for existing issues or create a new one to discuss your changes
2. **Create a branch**: Branch from `master` with a descriptive name
   ```bash
   git checkout -b feature/add-customer-search
   git checkout -b fix/avatar-upload-bug
   ```

### Making Changes

1. **Write clean code**
   - Follow existing code style and conventions
   - Keep methods small and focused
   - Use meaningful variable and method names
   - Add comments only when necessary (prefer self-documenting code)

2. **Write tests**
   - Unit tests for services and utility classes
   - Integration tests for repositories
   - Controller tests for endpoints
   - Aim for good test coverage of your changes

3. **Run CI checks locally**
   ```bash
   ./scripts/ci.sh
   ```
   
   This script runs:
   - Secret scanning (git-secrets or gitleaks)
   - Maven tests (`mvn verify`)
   - Docker Compose config validation
   - Optional: Docker image build (with `DOCKER_BUILD=1`)

4. **Test manually**
   - Start the application locally
   - Test your changes in the browser
   - Verify all affected roles and permissions
   - Test error cases

### Committing Changes

1. **Write clear commit messages**
   ```
   Add customer search functionality
   
   - Add search form to customers list page
   - Implement CustomerRepository.searchByName()
   - Add integration tests for search
   - Update documentation
   ```

2. **Commit frequently**
   - Make small, logical commits
   - Each commit should represent one coherent change
   - Don't combine multiple unrelated changes

3. **Never commit secrets**
   - `.env` files are in `.gitignore`
   - Never commit passwords, API keys, or tokens
   - The CI script checks for secrets before tests

### Submitting a Pull Request

1. **Push your branch**
   ```bash
   git push origin feature/add-customer-search
   ```

2. **Create a pull request**
   - Use a clear, descriptive title
   - Describe what your changes do and why
   - Reference related issues: "Fixes #123" or "Related to #456"
   - Include screenshots for UI changes

3. **Respond to feedback**
   - Be open to suggestions and code review feedback
   - Make requested changes in new commits
   - Push updates to the same branch

4. **Merge requirements**
   - All tests must pass
   - Code review approval required
   - No merge conflicts with `master`

## Code Style Guidelines

### Java Code

- **Formatting**: Follow standard Java conventions
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Aim for 120 characters max
- **Naming**:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Annotations**: Use Lombok judiciously (`@Getter`, `@Setter`, `@Builder`)

### Package Structure

```
app/
├── config/          # Configuration classes
├── controller/      # REST and view controllers
├── model/
│   ├── dto/        # Data Transfer Objects
│   ├── entity/     # JPA entities
│   └── enums/      # Enumerations
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
│   └── impl/       # Service implementations
├── security/        # Security configuration
├── util/           # Utility classes
└── validation/     # Custom validators
```

### Testing

- **Unit tests**: Test individual methods in isolation
- **Integration tests**: Use `@SpringBootTest` for repository and service tests
- **Test naming**: `methodName_scenario_expectedResult()`
  ```java
  @Test
  void createCustomer_validData_returnsCustomer() { ... }
  
  @Test
  void createCustomer_duplicateEmail_throwsException() { ... }
  ```
- **Test data**: Use builders or test fixtures
- **Assertions**: Prefer AssertJ for readable assertions

### Documentation

- **JavaDoc**: Required for public APIs and complex methods
- **README updates**: Update README.md if you add features or change setup
- **Database migrations**: Document any schema changes

## Database Migrations

### Flyway Guidelines

All database schema changes must be done through Flyway migrations:

1. **Create a new migration file**
   ```
   src/main/resources/db/migration/V<version>__<description>.sql
   ```
   
   Examples:
   - `V2__add_customer_phone.sql`
   - `V3__create_notifications_table.sql`
   - `V4__add_user_last_login.sql`

2. **Version numbering**
   - Increment from the last version
   - Use integers: V1, V2, V3, ...
   - Never modify existing migration files

3. **Migration content**
   ```sql
   -- V2__add_customer_phone.sql
   ALTER TABLE customers ADD COLUMN phone VARCHAR(20);
   ```

4. **Test migrations**
   - Start with a clean database
   - Run the application to apply migrations
   - Verify the changes in MySQL
   - Test rollback if needed (manual for Flyway Community)

5. **Never:**
   - Modify an existing migration that has been committed
   - Skip version numbers
   - Use `ddl-auto=update` in production (it's set to `validate`)

### Why Only New Files?

Once a migration is applied to any database (dev, staging, prod), its checksum is recorded. Modifying it will cause Flyway to reject the migration on other databases.

**If you need to change a migration:**
- If not yet merged: Delete it and create a new one
- If already merged: Create a new migration to modify the schema

## Testing Guidelines

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerServiceTest

# Run specific test method
mvn test -Dtest=CustomerServiceTest#createCustomer_validData_returnsCustomer

# Run tests with coverage
mvn verify

# Run integration tests only
mvn test -P integration-tests
```

### Test Categories

1. **Unit Tests**: Fast, isolated tests
   ```java
   @ExtendWith(MockitoExtension.class)
   class CustomerServiceUnitTest {
       @Mock
       private CustomerRepository customerRepository;
       
       @InjectMocks
       private CustomerServiceImpl customerService;
       
       @Test
       void createCustomer_validData_returnsCustomer() {
           // Arrange
           CustomerCreateDTO dto = CustomerCreateDTO.builder()
               .name("Test Customer")
               .email("test@example.com")
               .build();
           
           // Act
           Customer result = customerService.createCustomer(dto);
           
           // Assert
           assertThat(result).isNotNull();
           assertThat(result.getName()).isEqualTo("Test Customer");
       }
   }
   ```

2. **Integration Tests**: Test with real Spring context
   ```java
   @SpringBootTest
   @Transactional
   class CustomerServiceTest {
       @Autowired
       private CustomerService customerService;
       
       @Test
       void createCustomer_savesToDatabase() {
           // Test with real database and transaction rollback
       }
   }
   ```

3. **Controller Tests**: Test HTTP endpoints
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   class CustomerControllerTest {
       @Autowired
       private MockMvc mockMvc;
       
       @Test
       @WithMockUser(roles = "ADMIN")
       void getCustomers_asAdmin_returnsCustomersList() throws Exception {
           mockMvc.perform(get("/customers"))
               .andExpect(status().isOk())
               .andExpect(view().name("customers/list"));
       }
   }
   ```

### Test Data

Use demo data seeder as inspiration, or create test fixtures:

```java
public class TestDataBuilder {
    public static User createAdminUser() {
        return User.builder()
            .username("admin")
            .email("admin@example.com")
            .password("encoded_password")
            .role(Role.ADMIN)
            .active(true)
            .build();
    }
}
```

## Common Tasks

### Adding a New Feature

1. Create an issue describing the feature
2. Create a feature branch: `feature/feature-name`
3. Implement the feature with tests
4. Update documentation (README, this guide, etc.)
5. Run `./scripts/ci.sh`
6. Submit a pull request

### Fixing a Bug

1. Create an issue (if one doesn't exist)
2. Create a fix branch: `fix/bug-description`
3. Write a test that reproduces the bug
4. Fix the bug (test should now pass)
5. Run `./scripts/ci.sh`
6. Submit a pull request

### Adding a New Role Permission

1. Update `app/model/enums/Role.java` if adding a new role
2. Add `@PreAuthorize` annotations to controllers/services
3. Update Thymeleaf templates with `sec:authorize`
4. Write tests for all role combinations
5. Update `docs/ROLES.md`

### Adding Internationalization

1. Add keys to all message files:
   - `src/main/resources/messages.properties` (English default)
   - `src/main/resources/messages_bg.properties` (Bulgarian)
   - `src/main/resources/messages_de.properties` (German)

2. Use in Thymeleaf templates:
   ```html
   <h1 th:text="#{page.title}">Default Title</h1>
   ```

3. Use in Java code:
   ```java
   @Autowired
   private MessageSource messageSource;
   
   String message = messageSource.getMessage("key", null, locale);
   ```

## Pull Request Checklist

Before submitting your PR, ensure:

- [ ] Code follows project style guidelines
- [ ] All tests pass locally (`mvn verify`)
- [ ] New code has appropriate test coverage
- [ ] CI script passes (`./scripts/ci.sh`)
- [ ] No secrets or sensitive data committed
- [ ] Database migrations are in new files only
- [ ] Documentation updated (README, ROLES.md, etc.)
- [ ] Commit messages are clear and descriptive
- [ ] PR description explains what and why

## Getting Help

- **Questions**: Open a GitHub issue with the "question" label
- **Bugs**: Open a GitHub issue with the "bug" label
- **Features**: Open a GitHub issue with the "enhancement" label
- **Security**: Contact maintainers directly (see [SECURITY.md](SECURITY.md))

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Welcome newcomers and help them get started
- Assume good intentions

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## Recognition

Contributors will be recognized in the project README. Thank you for helping improve the Test Purchase Application!
