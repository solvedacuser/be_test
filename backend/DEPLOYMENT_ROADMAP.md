# GitHub Actions + Docker Hub + EC2 배포 로드맵

## 0. 현재 결정된 기준

- 배포 방식: 간단 배포
- 배포 대상: 백엔드 서버
- 서버: AWS EC2
- EC2 OS: Ubuntu
- 컨테이너: Docker
- CI/CD: GitHub Actions
- Docker 이미지 저장소: Docker Hub
- Docker Hub 이미지: `token12345/likelion-backend`
- 배포 브랜치: `testbe`
- 배포 방식: GitHub Actions에서 EC2로 SSH 접속 후 Docker Compose 실행
- 백엔드 포트: `8081:8080`
- DB 위치: EC2 내부 Docker 컨테이너
- DB 종류: MySQL
- 도메인/HTTPS: 도메인 없이 EC2 public IP로 접속

배포 흐름:

```text
testbe 브랜치 push
-> GitHub Actions 실행
-> Docker image build
-> Docker Hub push
-> EC2 SSH 접속
-> docker compose pull
-> docker compose up -d
```

## 1. 먼저 확정해야 할 항목

### 1.1 Docker Hub repository 이름

결정사항:

```text
token12345/likelion-backend
```

확정 후 GitHub Actions와 EC2의 `docker-compose.yml`에서 동일하게 사용한다.

### 1.2 백엔드 실행 포트

결정사항:

```text
8081:8080
```

적용 위치:

- Spring Boot 서버 포트
- Docker 컨테이너 포트
- EC2 보안 그룹 인바운드 규칙
- Docker Compose ports 설정

현재는 도메인과 Nginx 없이 EC2 public IP의 `8081` 포트로 직접 접속한다.

### 1.3 EC2 OS와 SSH 유저

결정사항:

```text
EC2 OS=Ubuntu
```

Ubuntu 기준 SSH 유저:

```text
EC2_USER=ubuntu
```

서버 작업 디렉토리 예시:

```text
/home/ubuntu/backend
```

### 1.4 DB 위치

결정사항:

```text
DB는 EC2 내부 Docker 컨테이너로 실행
DB 종류는 MySQL 사용
```

정해야 할 추가 항목:

- DB username/password/database name
- 백엔드에서 사용할 DB 접속 URL

확정 기준:

```text
Docker Compose DB 서비스 이름=mysql
Docker Compose 내부 DB 접속=mysql:3306
로컬 직접 DB 접속=localhost:3307
```

### 1.5 도메인과 HTTPS 여부

결정사항:

```text
도메인 없이 접속
```

접속 방식:

```text
http://EC2_PUBLIC_IP:8081
```

초기 배포에서는 Nginx, Certbot, HTTPS 설정을 하지 않는다.

## 2. Docker Hub 준비

결정사항:

```text
Docker 이미지 저장소=Docker Hub
Docker Hub repository=token12345/likelion-backend
Docker Hub 비밀번호=계정 비밀번호가 아니라 access token 사용
```

1. Docker Hub 계정을 준비한다.
2. Docker Hub에서 백엔드용 repository를 생성한다.
3. Docker Hub access token을 생성한다.
4. 아래 값을 기록한다.

```text
DOCKERHUB_USERNAME=
DOCKERHUB_TOKEN=
DOCKER_IMAGE=token12345/likelion-backend
```

확정값:

```text
DOCKERHUB_USERNAME=token12345
DOCKER_IMAGE=token12345/likelion-backend
```

## 3. EC2 준비

1. AWS EC2 인스턴스를 생성한다.
2. OS는 Ubuntu로 생성한다.
3. 보안 그룹 인바운드 규칙을 설정한다.

결정사항:

```text
EC2 OS=Ubuntu
외부 접속 포트=8081
도메인 사용 안 함
```

최소 인바운드 규칙:

```text
22   SSH
8081 Backend
```

4. EC2에 SSH로 접속한다.
5. Docker를 설치한다.
6. Docker Compose를 사용할 수 있는지 확인한다.

```bash
docker --version
docker compose version
```

7. 배포 디렉토리를 생성한다.

```bash
mkdir -p /home/ubuntu/backend
```

## 4. EC2 환경변수 파일 준비

EC2 서버의 배포 디렉토리에 `.env` 파일을 만든다.

결정사항:

```text
백엔드와 DB는 같은 EC2의 Docker Compose 네트워크에서 통신
```

예시:

```bash
cd /home/ubuntu/backend
vi .env
```

`.env` 예시:

```env
SPRING_PROFILES_ACTIVE=docker
MYSQL_DATABASE=
MYSQL_USER=
MYSQL_PASSWORD=
MYSQL_ROOT_PASSWORD=
JWT_SECRET=
```

MySQL 접속 정보는 Spring profile 설정 파일에서 환경변수로 주입한다.

### application-local.yml

로컬에서 직접 MySQL을 실행할 때는 `localhost:3307`로 접속한다.

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3307/${MYSQL_DATABASE}?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
```

### application-docker.yml

Docker Compose 내부에서 컨테이너끼리 통신할 때는 DB 컨테이너의 서비스 이름인 `mysql`을 사용한다.

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
```

주의:

- `.env`는 GitHub에 올리지 않는다.
- 운영 비밀번호, JWT secret, DB 접속 정보는 EC2 서버에만 둔다.

## 5. docker-compose.yml 준비

EC2 서버의 `/home/ubuntu/backend/docker-compose.yml`에 작성한다.

결정사항:

```text
백엔드 포트=8081:8080
DB=EC2 내부 Docker 컨테이너
DB 종류=MySQL
DB 서비스 이름=mysql
```

기본 구조 예시:

```yaml
services:
  backend:
    image: token12345/likelion-backend:latest
    container_name: backend
    ports:
      - "8081:8080"
    env_file:
      - .env
    restart: always
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    container_name: backend-mysql
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
    restart: always

volumes:
  mysql-data:
```

주의:

- 백엔드는 Docker Compose 내부에서 `mysql:3306`으로 DB에 접속한다.
- 로컬에서 직접 MySQL에 접속할 때는 `localhost:3307` 기준으로 profile을 분리한다.
- DB 비밀번호는 실제 운영 값으로 바꾸고 GitHub에 올리지 않는다.
- Docker Compose의 `image` 값은 `token12345/likelion-backend:latest`를 사용한다.

## 6. GitHub Secrets 등록

GitHub repository의 `Settings > Secrets and variables > Actions`에 등록한다.

필수:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
EC2_HOST
EC2_USER
EC2_SSH_KEY
```

값 설명:

```text
DOCKERHUB_USERNAME = Docker Hub 계정명
DOCKERHUB_TOKEN    = Docker Hub 계정 비밀번호가 아니라 access token
EC2_HOST           = EC2 public IP
EC2_USER           = EC2 SSH 유저명, Ubuntu면 ubuntu
EC2_SSH_KEY        = EC2 접속용 private key 내용
```

## 7. Dockerfile 준비

프로젝트 루트에 `Dockerfile`을 둔다.

결정사항:

```text
백엔드 컨테이너 내부 포트=8080
```

Spring Boot Gradle 프로젝트 기준 예시:

```dockerfile
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

프로젝트 Java 버전에 맞게 base image만 조정한다. 서버 포트는 `8080` 기준으로 둔다.

## 8. GitHub Actions workflow 작성

아래 경로에 workflow 파일을 만든다.

결정사항:

```text
배포 브랜치=testbe
이미지 저장소=Docker Hub
배포 방식=EC2 SSH 접속 후 docker compose 실행
```

```text
.github/workflows/deploy.yml
```

기본 예시:

```yaml
name: Deploy Backend

on:
  push:
    branches:
      - testbe

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            token12345/likelion-backend:latest
            token12345/likelion-backend:${{ github.sha }}

      - name: Deploy to EC2
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.EC2_HOST }}
          username: ${{ secrets.EC2_USER }}
          key: ${{ secrets.EC2_SSH_KEY }}
          script: |
            cd /home/ubuntu/backend
            docker compose pull
            docker compose down
            docker compose up -d
            docker image prune -f
```

주의:

- Docker Hub repository 이름은 `token12345/likelion-backend`를 사용한다.
- Docker Hub 로그인 비밀번호는 GitHub Secret `DOCKERHUB_TOKEN`에 저장한 access token을 사용한다.

## 9. 첫 배포 전 수동 확인

EC2에서 먼저 Docker Compose가 정상 동작하는지 확인한다.

결정사항:

```text
EC2 내부 확인=http://localhost:8081
외부 확인=http://EC2_PUBLIC_IP:8081
```

```bash
cd /home/ubuntu/backend
docker compose pull
docker compose up -d
docker ps
docker logs backend
docker logs backend-mysql
```

서버 응답 확인:

```bash
curl http://localhost:8081
```

외부에서 확인:

```text
http://EC2_PUBLIC_IP:8081
```

## 10. 자동 배포 확인

결정사항:

```text
testbe 브랜치 push 시 자동 배포
```

1. `testbe` 브랜치에 코드를 push한다.
2. GitHub Actions 탭에서 workflow 실행 여부를 확인한다.
3. Docker Hub에 이미지가 push되었는지 확인한다.
4. EC2에서 컨테이너가 새로 실행되었는지 확인한다.

```bash
docker ps
docker images
docker logs backend
```

## 11. 배포 실패 시 확인 순서

### 11.1 GitHub Actions 실패

확인할 것:

- Docker Hub 로그인 실패 여부
- Docker build 실패 여부
- Docker push 실패 여부
- EC2 SSH 접속 실패 여부
- `testbe` 브랜치에 push했는지 여부

### 11.2 EC2에서 컨테이너 실행 실패

확인 명령어:

```bash
docker ps -a
docker logs backend
docker compose logs
```

주요 원인:

- `.env` 누락
- DB 접속 실패
- 포트 충돌
- Docker image 이름 불일치
- Java 버전 불일치

### 11.3 외부 접속 실패

확인할 것:

- EC2 보안 그룹 인바운드 포트
- 애플리케이션 실행 포트
- Docker Compose ports 설정
- 서버 내부에서는 응답하지만 외부에서만 안 되는지 여부

## 12. 이후 추가할 수 있는 것

초기 배포 성공 후 필요할 때 추가한다.

- Nginx reverse proxy
- 도메인 연결
- Certbot HTTPS
- RDS 분리
- S3 파일 업로드
- CloudWatch 로그
- Slack 또는 Discord 배포 알림
- 무중단 배포
- 롤백 스크립트
