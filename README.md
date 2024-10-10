# SurePay Validator

Validate your transaction's uniqueness and correctness then see the report immediately after uploading your
transactions!

## How to run

#### (Recommended) Using `docker compose`:

```shell
docker compose up
```

#### Using IntelliJ:

1. Copy `.example.env` to `.env`.
2. Fill in the values of each environment variable in `.env`. Example:
    ```text
    SPRING_PROFILES_ACTIVE=local
    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/surepay_validator
    SPRING_DATASOURCE_USERNAME=postgres
    SPRING_DATASOURCE_PASSWORD=12345678
    ```
3. Open `nl.surepay.validator.SurepayApplication`.
4. Click the play button beside the class name.
5. Select `Modify Run Configuration...`.
6. Click the folder icon beside `Environment variables` and point it to `.env`.

> TIP: If you have the **Ultimate** version of Intellij, just run this project as a Spring Boot project.

## APIs.

A sample postman collection is included under the `docs` folder. Import that collection and use it after running the
application.

> **POST /api/v1/uploads**
>
> Upload the csv/json file and return the report based on the validations.

### Why is the report generated immediately after uploading the file?

Initially, I thought of creating a separate table to save the report and another API to download the report later.
But it would take time to get the report if the uploaded file is too large. It will save time to just stream the report
back to the consumer with the same file type as what was uploaded.

## Technologies Used

1. Spring Boot
2. Spring Data JDBC
3. PostgreSQL
4. Docker
5. Lombok
6. Testcontainers
7. OpenCSV for csv files
8. Jackson for json files
