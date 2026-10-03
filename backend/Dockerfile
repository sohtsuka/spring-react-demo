# 本番構成の API イメージ。
# ビルド → 実行を分け、実行イメージには JRE と jar だけを置く。

FROM docker.io/library/eclipse-temurin:25-jdk AS build
WORKDIR /build

# 依存の解決結果をレイヤーキャッシュに残すため、まずビルド定義だけを入れる
COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle gradle.lockfile settings-gradle.lockfile ./
COPY config config
RUN ./gradlew --no-daemon --console=plain dependencies --configuration runtimeClasspath > /dev/null

COPY src src
# 検証は CI で実施済みのため、ここでは jar を作るだけにする
RUN ./gradlew --no-daemon --console=plain bootJar -x test \
    && cp build/libs/*.jar /build/app.jar

FROM docker.io/library/eclipse-temurin:25-jre
WORKDIR /app

# root では動かさない
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app
USER 10001:10001

COPY --from=build /build/app.jar app.jar

EXPOSE 8080
# APP_PEPPER は未設定だと起動しない (本番で必須)。DB_URL などは compose 側で渡す。
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
