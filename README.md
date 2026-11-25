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
# .env의 DB_USERNAME, DB_PASSWORD, AT_SERVICE_KEY, KMI_SERVICE_KEY를 입력
docker compose up -d postgres
./gradlew bootRun
```

로컬에서는 Compose와 Spring의 `local` 프로필이 같은 `.env` 파일을 읽습니다. DB는 기본적으로 `127.0.0.1:5432`에 노출됩니다. `.env`는 Git에서 제외됩니다.

`prod` 프로필은 `.env` 파일을 자동으로 읽지 않습니다. 배포 환경에는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AT_SERVICE_KEY`, `KMI_SERVICE_KEY`를 환경 변수로 전달하고 `prod` 프로필로 실행합니다. `.env.example`은 필요한 변수의 예시이며 실제 인증키와 비밀번호는 Git에 올리지 않습니다.

```sh
./gradlew bootRun --args='--spring.profiles.active=prod'
```

현재 전체 테스트와 빌드에는 개발 DB가 필요합니다.

```sh
./gradlew test
./gradlew build
```

종료할 때는 `docker compose down`을 실행합니다. 로컬 DB 데이터는 Compose 볼륨에 유지됩니다.

현재는 개발 스키마와 Flyway 마이그레이션 파일이 없습니다. 설계 정책에 따라 Hibernate 자동 DDL과 앱 시작 시 Flyway 실행을 비활성화했습니다. 테이블이 필요한 기능을 추가할 때는 개발 스키마 SQL을 함께 관리해야 합니다.

## aT 정산정보 API 수집

`AT_SERVICE_KEY`에는 공공데이터포털에서 발급받은 실제 인증키를 입력합니다. `.env.example`의 설명용 값으로는 API를 호출할 수 없습니다. aT API 주소, 페이지 크기와 연결·응답 시간 제한은 `src/main/resources/application.yaml`에서 설정합니다.

`AtCollector`는 정산일자 하루, 시장 코드, 품목 대·중분류 코드와 페이지 번호를 받아 한 페이지를 수집합니다. 결과에는 원본 응답, 이번 페이지의 항목 수와 다음 페이지 요청 조건이 담깁니다. HTTP 오류, 제공기관 오류와 잘못된 응답은 구분해 전달합니다. 원본 응답을 저장하고 Request 상태를 갱신하는 모듈은 별도 작업입니다.

## KMI 유통정보 API 수집

`KMI_SERVICE_KEY`에는 KMI API 인증키를 입력합니다. `.env.example`의 설명용 값으로는 API를 호출할 수 없습니다. KMI API 주소와 연결·응답 시간 제한은 `src/main/resources/application.yaml`에서 설정합니다.

`KmiCollector`는 어종 코드, 가격 조회 작업, 시작일과 종료일을 받아 한 기간을 수집합니다. 가격 조회 작업은 산지·도매·소매 중에서 선택합니다. 결과에는 원본 응답과 데이터 건수가 담기며 후속 페이지 요청은 만들지 않습니다. HTTP 오류와 잘못된 응답은 구분하지만, 제공기관 오류 형식은 실제 응답을 확인한 뒤 처리할 예정입니다.

KMI 호스트 연결이 시간 초과되어 실제 인증 성공과 산지·도매·소매 응답 구조는 아직 검증하지 못했습니다. 원본 응답 저장과 Request 상태 갱신은 별도 작업입니다.
