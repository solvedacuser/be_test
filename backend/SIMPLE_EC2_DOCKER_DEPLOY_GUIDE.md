# EC2 + Docker Hub + GitHub Actions 간단 배포 가이드

## 0. 먼저 정할 값

프로젝트마다 아래 값을 먼저 정한다.

```text
DEPLOY_BRANCH=배포할 브랜치명
DOCKER_IMAGE=DockerHub아이디/이미지이름
EC2_USER=ubuntu
APP_DIR=/home/ubuntu/프로젝트디렉토리
HOST_PORT=외부접속포트
CONTAINER_PORT=앱내부포트
DB_NAME=데이터베이스명
DB_USER=DB유저명
DB_PASSWORD=DB비밀번호
```

예시:

```text
DEPLOY_BRANCH=testbe
DOCKER_IMAGE=token12345/likelion-backend
APP_DIR=/home/ubuntu/backend
HOST_PORT=8081
CONTAINER_PORT=8080
DB_NAME=test12
DB_USER=likelion
```

## 1. EC2 준비

AWS 콘솔에서 Ubuntu EC2를 생성한다.

키 페어:

```text
RSA
.pem
```

보안 그룹 인바운드:

```text
22          SSH
HOST_PORT   Backend
```

예시:

```text
22    SSH
8081  Backend
```

EC2 접속:

```bash
ssh -i key.pem ubuntu@EC2_PUBLIC_IP
```

Docker 설치:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker ubuntu
```

권한 적용을 위해 SSH를 재접속한다.

## 2. EC2 배포 디렉토리

EC2에서 실행:

```bash
mkdir -p ~/프로젝트디렉토리
cd ~/프로젝트디렉토리
```

필요한 파일:

```text
APP_DIR
├── compose.yml
└── .env
```

주의:

```text
.env~ 는 잘못된 파일명
.env 여야 Docker Compose가 읽음
```

## 3. EC2 .env

```bash
nano .env
```

예시:

```env
SPRING_PROFILES_ACTIVE=docker

MYSQL_DATABASE=DB_NAME
MYSQL_USER=DB_USER
MYSQL_PASSWORD=DB_PASSWORD
MYSQL_ROOT_PASSWORD=DB_ROOT_PASSWORD

JWT_SECRET=32바이트_이상의_긴_랜덤_문자열
JWT_ACCESS_TOKEN_EXPIRATION_MS=900000
JWT_REFRESH_TOKEN_EXPIRATION_MS=1209600000
CORS_ALLOWED_ORIGIN=http://localhost:3000
```

JWT secret 생성:

```bash
openssl rand -base64 32
```

저장:

```text
Ctrl + O
Enter
Ctrl + X
```

## 4. EC2 compose.yml

```bash
nano compose.yml
```

```yaml
services:
  backend:
    image: DOCKER_IMAGE:latest
    container_name: backend
    restart: always
    ports:
      - "HOST_PORT:CONTAINER_PORT"
    env_file:
      - .env
    environment:
      SPRING_PROFILES_ACTIVE: docker
    depends_on:
      mysql:
        condition: service_healthy

  mysql:
    image: mysql:8.0
    container_name: backend-mysql
    restart: always
    ports:
      - "127.0.0.1:3307:3306"
    env_file:
      - .env
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 10

volumes:
  mysql-data:
```

예시:

```yaml
image: token12345/likelion-backend:latest
ports:
  - "8081:8080"
```

## 5. 로컬 프로젝트 파일

프로젝트 루트에 둔다.

```text
Dockerfile
.dockerignore
compose.yml
.env.example
.github/workflows/deploy.yml
```

`.env`는 Git에 올리지 않는다.

`.gitignore`:

```text
.env
.env.*
!.env.example
```

## 6. Dockerfile 예시

Java 21 Spring Boot 예시:

```dockerfile
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

`EXPOSE`는 앱 내부 포트와 맞춘다.

## 7. GitHub Secrets

GitHub 저장소에서 등록:

```text
Settings
→ Secrets and variables
→ Actions
```

필수:

```text
DOCKERHUB_USERNAME=Docker Hub 아이디
DOCKERHUB_TOKEN=Docker Hub access token
EC2_HOST=EC2 Public IPv4
EC2_USER=ubuntu
EC2_SSH_KEY=.pem 파일 전체 내용
```

## 8. GitHub Actions

workflow 위치:

```text
.github/workflows/deploy.yml
```

핵심 설정:

```yaml
on:
  push:
    branches:
      - DEPLOY_BRANCH
```

저장소 루트가 상위 폴더이고 백엔드가 `backend/` 안에 있으면:

```yaml
working-directory: backend
context: ./backend
file: ./backend/Dockerfile
```

저장소 루트가 백엔드 프로젝트 자체이면:

```yaml
context: .
file: ./Dockerfile
```

EC2 배포 명령:

```bash
cd APP_DIR
docker compose -f compose.yml pull backend
docker compose -f compose.yml up -d --remove-orphans
docker image prune -f
```

## 9. 배포 실행

배포 브랜치에 push:

```bash
git push origin DEPLOY_BRANCH
```

GitHub에서 확인:

```text
Actions → Deploy Backend
```

## 10. 배포 확인

EC2에서:

```bash
cd APP_DIR
docker compose -f compose.yml ps
```

정상:

```text
backend         Up
backend-mysql   Up (healthy)
```

내부 확인:

```bash
curl http://localhost:HOST_PORT/actuator/health
```

외부 접속:

```text
http://EC2_PUBLIC_IP:HOST_PORT
http://EC2_PUBLIC_IP:HOST_PORT/swagger-ui.html
```

## 11. 자주 나는 문제

`.env`를 못 읽는 경우:

```text
MYSQL_DATABASE variable is not set
```

확인:

```bash
ls -al
```

`.env~`가 아니라 `.env`여야 한다.

JWT secret이 짧은 경우:

```text
WeakKeyException
key size must be >= 256 bits
```

해결:

```bash
openssl rand -base64 32
```

MySQL 비밀번호를 바꾼 경우:

```text
Access denied for user
```

초기 배포라 데이터 삭제 가능하면:

```bash
docker compose -f compose.yml down -v
docker compose -f compose.yml up -d
```

메모리 부족:

```text
Restarting (137)
```

swap 추가:

```bash
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

