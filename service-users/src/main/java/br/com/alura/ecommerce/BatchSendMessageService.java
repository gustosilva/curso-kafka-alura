package br.com.alura.ecommerce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchSendMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSendMessageService.class.getName());
    private final Connection connection;

    BatchSendMessageService() throws SQLException {
        String url = "jdbc:sqlite::resource:users_database.db";
        this.connection = DriverManager.getConnection(url);
        connection.createStatement().execute("create table if not exists Users (" +
                "uuid varchar(200) primary key," +
                "email varchar(200))");
    }

    public static void main(String[] args) throws SQLException {
        BatchSendMessageService batchSendMessageService = new BatchSendMessageService();
        try (KafkaService<String> kafkaService = new KafkaService<>(BatchSendMessageService.class.getSimpleName(),
                "SEND_MESSAGE_TO_ALL_USERS",
                batchSendMessageService::parse,
                String.class,
                new HashMap<>())) {
            kafkaService.run();
        }
    }

    private final KafkaDispatcher<User> batchDispatcher = new KafkaDispatcher<>();

    private void parse(ConsumerRecord<String, String> record) throws ExecutionException, InterruptedException, SQLException {
        LOGGER.info("--------------------------------------------");
        LOGGER.info("Processing new batch");
        LOGGER.info("Topic: " + record.value());

        for (User user : getAllUsers()) {
            batchDispatcher.send(record.value(), user.getUuid(), user);
        }

    }

    private List<User> getAllUsers() throws SQLException {
        ResultSet results = connection.prepareStatement("select uuid from Users").executeQuery();
        List<User> users = new ArrayList<>();
        while (results.next()) {
            users.add(new User(results.getString(1)));
        }
        return users;
    }
}