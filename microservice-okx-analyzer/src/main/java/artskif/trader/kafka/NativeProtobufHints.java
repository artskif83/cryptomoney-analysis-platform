package artskif.trader.kafka;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(
        targets = {
                // Confluent Protobuf SerDe
                io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class,
                io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer.class,

                // Контекст-стратегия (как раз та, что падает)
                io.confluent.kafka.serializers.context.NullContextNameStrategy.class,

                // Subject Name стратегии (часто подтягиваются конфигом)
                io.confluent.kafka.serializers.subject.TopicNameStrategy.class,
                io.confluent.kafka.serializers.subject.RecordNameStrategy.class,
                io.confluent.kafka.serializers.subject.TopicRecordNameStrategy.class,
                io.confluent.kafka.serializers.subject.DefaultReferenceSubjectNameStrategy.class,

                // Твои protobuf-модели
                my.signals.v1.Signal.class
        },
        methods = true,   // важно: сохранит конструкторы
        fields = false,
        ignoreNested = false
)
public final class NativeProtobufHints { }