package com.example.app.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.List;

/**
 * Spring Boot 4 では Flyway 自動設定が削除されたため手動設定する。
 * ロケーションは application.yml / application-dev.yml の
 * spring.flyway.locations から注入する。
 */
@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource, Environment env) {
        // YAML リストは Environment#getProperty で取得できないため Binder を使う
        List<String> locations = Binder.get(env)
                .bind("spring.flyway.locations", Bindable.listOf(String.class))
                .orElse(List.of("classpath:db/migration"));

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations.toArray(String[]::new))
                .load();

        // マイグレーションファイルが変更された場合のチェックサムを更新する
        // (開発環境でテストデータを修正した際などに対応)
        flyway.repair();
        flyway.migrate();

        return flyway;
    }
}
