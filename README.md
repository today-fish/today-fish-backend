# 오늘의 수산 백엔드

Spring Boot 4.1.2, Java 25, Gradle 9.9.1을 사용하는 단일 프로젝트입니다.

## 로컬 개발환경

- Java 25 (팀 표준: Amazon Corretto 25)
- Docker Desktop 또는 Docker Engine과 Docker Compose
- 별도 Gradle 설치는 필요하지 않습니다. 저장소의 Wrapper를 사용합니다.

터미널에서 Java 25가 선택됐는지 확인합니다.

```sh
java -version
./gradlew --version
```

macOS에서 다른 Java 버전이 기본으로 선택된다면 실행할 터미널에서 Java 25를 지정합니다.

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
```

Docker를 실행한 다음 저장소 루트에서 개발 DB와 앱을 시작합니다.

```sh
cp .env.example .env
# .env의 DB_USERNAME과 DB_PASSWORD를 입력
docker compose up -d postgres
./gradlew bootRun
```

로컬에서는 Compose와 Spring의 `local` 프로필이 같은 `.env` 파일을 읽습니다. DB는 기본적으로 `127.0.0.1:5433`에 노출됩니다. `.env`는 Git에서 제외됩니다.

배포 서버에서도 `.env.example`을 `.env`로 복사해 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 채웁니다. 앱은 `prod` 프로필을 지정해 실행합니다. 예: `./gradlew bootRun --args='--spring.profiles.active=prod'`. 배포용 `.env`는 Git에 올리지 않습니다.

빌드와 테스트도 같은 DB가 실행 중이어야 합니다.

```sh
./gradlew test
./gradlew build
```

종료할 때는 `docker compose down`을 실행합니다. 로컬 DB 데이터는 Compose 볼륨에 유지됩니다.

현재는 개발 스키마와 Flyway 마이그레이션 파일이 없습니다. 설계 정책에 따라 Hibernate 자동 DDL과 앱 시작 시 Flyway 실행을 비활성화했습니다. 테이블이 필요한 기능을 추가할 때는 개발 스키마 SQL을 함께 관리해야 합니다.
