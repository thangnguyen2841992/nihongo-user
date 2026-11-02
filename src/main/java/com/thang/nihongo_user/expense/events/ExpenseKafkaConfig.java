package com.thang.nihongo_user.expense.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** The producer template and realtime listener factory are shared with the wallet ({@code WalletKafkaConfig}). */
@Configuration
public class ExpenseKafkaConfig {
    @Bean
    public NewTopic expenseEventsTopic(@Value("${expense.events.topic:expense.events.v1}") String topic,
                                       @Value("${expense.events.partitions:3}") int partitions,
                                       @Value("${expense.events.replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }
}
