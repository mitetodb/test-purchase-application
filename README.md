Test Purchase Application е система за управление на тестови покупки (Test Purchases), използвана от компании, които извършват Mystery Shopping поръчки за различни клиенти. Допълнително е изграден отделен Price Calculation микросървис, който предоставя REST API за калкулации на разходи. Основното приложение комуникира с него чрез JSON заявки. 

Стъпки при стартиране:

1.start mysql with docker:
docker run --name mysql8_db -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=testpurchase_app -d mysql:8

2.start rest price-calculation-microservice
mvn spring-boot:run

3.start the main test-purchase-application
mvn spring-boot:run

Link to test purchase application:
https://github.com/mitetodb/test-purchase-application

Link to price calculation microservice:
https://github.com/mitetodb/price-calculation-microservice
