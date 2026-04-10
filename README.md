# 🌬️ Air Quality Monitor — Escolas

Backend Java/Spring Boot para monitoramento em tempo real da qualidade do ar em ambientes escolares.

---

## Sumário
- [Funcionalidades](#funcionalidades)
- [Stack tecnológica](#stack)
- [Estrutura do projeto](#estrutura)
- [Como rodar](#como-rodar)
- [API REST](#api-rest)
- [Cálculo do AQI](#cálculo-do-aqi)
- [Sistema de alertas](#sistema-de-alertas)
- [Poluentes monitorados](#poluentes-monitorados)

---

## Funcionalidades

| Módulo | Descrição |
|--------|-----------|
| **Ingestão de dados** | Endpoint REST para recebimento de leituras dos sensores IoT |
| **Cálculo AQI** | Algoritmo baseado nos breakpoints EPA 2024 + CONAMA 491/2018 |
| **Alertas automáticos** | Geração e notificação (e-mail + WebSocket) quando limites são ultrapassados |
| **Dashboard por escola** | Visão consolidada de todas as salas em tempo real |
| **Relatórios** | Relatórios diários e semanais em PDF |
| **Detecção de offline** | Scheduler detecta sensores que pararam de enviar dados |
| **Histórico** | Leituras particionadas por mês para alta performance em consultas |

---

## Stack

- **Java 21** (records, sealed classes, pattern matching)
- **Spring Boot 3.2** (Web, Data JPA, Security, WebSocket, Mail, Actuator)
- **PostgreSQL** com particionamento de tabelas por mês
- **Flyway** para migrações versionadas
- **JWT** para autenticação stateless
- **Lombok + MapStruct** para redução de boilerplate
- **OpenAPI 3 / Swagger UI** em `/swagger-ui.html`
- **JUnit 5 + AssertJ** para testes unitários

---

## Estrutura do projeto

```
air-quality-monitor/
├── src/main/java/br/com/escola/airquality/
│   ├── AirQualityMonitorApplication.java
│   ├── config/
│   │   ├── AirQualityProperties.java    # Propriedades tipadas do application.yml
│   │   ├── JwtAuthenticationFilter.java # Filtro JWT stateless
│   │   ├── SecurityConfig.java          # Spring Security — RBAC
│   │   └── WebSocketConfig.java         # STOMP para alertas em tempo real
│   ├── controller/
│   │   ├── SensorController.java        # POST /readings, GET /readings/latest
│   │   ├── AlertController.java         # GET/PATCH alertas
│   │   ├── SchoolController.java        # GET dashboard escolas
│   │   └── ReportController.java        # GET relatórios PDF
│   ├── domain/
│   │   ├── School.java
│   │   ├── Room.java
│   │   ├── Sensor.java
│   │   ├── SensorReading.java           # Entidade central com AQI calculado
│   │   └── Alert.java
│   ├── dto/
│   │   ├── SensorReadingRequest.java    # Payload do sensor (validado com @Valid)
│   │   ├── SensorReadingResponse.java
│   │   ├── AlertResponse.java
│   │   └── SchoolSummaryResponse.java
│   ├── exception/
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice centralizado
│   ├── repository/
│   │   ├── SensorReadingRepository.java # Consultas JPQL + native SQL
│   │   ├── SensorRepository.java
│   │   ├── AlertRepository.java
│   │   └── SchoolRepository.java
│   ├── scheduler/
│   │   └── AirQualityScheduler.java     # Detecção offline, limpeza, retry notificações
│   └── service/
│       ├── AqiCalculatorService.java    # Algoritmo AQI (poluentes EPA)
│       ├── SensorService.java           # Orquestração leitura → AQI → alerta
│       ├── AlertService.java            # Criação e gestão de alertas
│       ├── NotificationService.java     # E-mail + WebSocket
│       ├── SchoolService.java           # Dashboard consolidado
│       └── ReportService.java           # Geração de relatórios
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__initial_schema.sql
├── src/test/java/
│   └── AqiCalculatorServiceTest.java    # 12+ testes parametrizados
└── pom.xml
```

---

## Como rodar

### Pré-requisitos
- Java 21+
- Docker (para PostgreSQL)
- Maven 3.9+

### 1. Subir o banco de dados

```bash
docker run -d \
  --name air-quality-db \
  -e POSTGRES_DB=air_quality_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Configurar variáveis de ambiente

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=minha-chave-secreta-com-pelo-menos-256-bits-aqui
export MAIL_HOST=smtp.gmail.com
export MAIL_USERNAME=seu-email@gmail.com
export MAIL_PASSWORD=sua-senha-app
```

### 3. Executar

```bash
./mvnw spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.
Swagger UI disponível em `http://localhost:8080/swagger-ui.html`.

### 4. Rodar os testes

```bash
./mvnw test
```

---

## API REST

### Autenticação

Todas as requisições (exceto `/actuator/health` e docs) requerem o header:
```
Authorization: Bearer <jwt_token>
```

**Roles:**
- `SENSOR` — envio de leituras
- `SCHOOL_ADMIN` — dashboard, alertas e relatórios da escola
- `SYSTEM_ADMIN` — acesso total

---

### Registrar leitura (sensor → servidor)

```http
POST /api/v1/sensors/readings
Authorization: Bearer <sensor_token>
Content-Type: application/json

{
  "serial_number": "SN-2024-001",
  "co2_ppm": 950.5,
  "pm25_ugm3": 18.3,
  "pm10_ugm3": 35.1,
  "tvoc_mgm3": 0.45,
  "temperature_c": 23.2,
  "humidity_pct": 58.0,
  "no2_ppb": null,
  "recorded_at": "2026-04-10T09:15:00"
}
```

**Resposta 201:**
```json
{
  "id": 1042,
  "sensor_id": 1,
  "serial_number": "SN-2024-001",
  "room_name": "Sala 101",
  "school_name": "Escola Estadual João Pessoa",
  "co2_ppm": 950.5,
  "pm25_ugm3": 18.3,
  "pm10_ugm3": 35.1,
  "tvoc_mgm3": 0.45,
  "temperature_c": 23.2,
  "humidity_pct": 58.0,
  "no2_ppb": null,
  "aqi_value": 75,
  "aqi_category": "MODERATE",
  "aqi_label": "Qualidade do ar aceitável. Grupos muito sensíveis podem apresentar sintomas.",
  "recorded_at": "2026-04-10T09:15:00"
}
```

---

### Dashboard da escola

```http
GET /api/v1/schools/{schoolId}/dashboard
Authorization: Bearer <token>
```

---

### Alertas não resolvidos

```http
GET /api/v1/alerts/schools/{schoolId}

PATCH /api/v1/alerts/{alertId}/acknowledge
PATCH /api/v1/alerts/{alertId}/resolve
```

---

### Histórico de leituras

```http
GET /api/v1/sensors/{sensorId}/readings/history
  ?from=2026-04-01T00:00:00
  &to=2026-04-10T23:59:59
```

---

### WebSocket (alertas em tempo real)

Conectar via STOMP em `ws://localhost:8080/ws` e subscrever:
```
/topic/alerts/{schoolId}
```

---

## Cálculo do AQI

O AQI é calculado individualmente para cada poluente medido usando interpolação linear entre breakpoints:

```
AQI = ((IHi - ILo) / (BPHi - BPLo)) × (Cp - BPLo) + ILo
```

O **AQI geral** da sala é o **maior** AQI individual (pior poluente determina a categoria).

### Categorias

| AQI    | Categoria             | Ação recomendada |
|--------|-----------------------|------------------|
| 0–50   | GOOD                  | Atividades normais |
| 51–100 | MODERATE              | Grupos sensíveis atentos |
| 101–150| UNHEALTHY_SENSITIVE   | Crianças e asmáticos: reduzir exposição |
| 151–200| UNHEALTHY             | Considerar evacuação / ventilação forçada |
| 201–300| VERY_UNHEALTHY        | Evacuação recomendada |
| 301+   | HAZARDOUS             | EMERGÊNCIA — evacuar e acionar autoridades |

---

## Poluentes monitorados

| Poluente | Unidade | Padrão "Bom" | Referência |
|----------|---------|--------------|------------|
| CO₂      | ppm     | < 800        | ASHRAE 62.1 |
| PM2.5    | µg/m³   | < 12         | EPA 2024 |
| PM10     | µg/m³   | < 54         | EPA / CONAMA 491 |
| TVOC     | mg/m³   | < 0.3        | WHO guidelines |
| NO₂      | ppb     | < 53         | EPA NAAQS |
| Temperatura | °C  | 18–26        | ASHRAE 55 |
| Umidade relativa | % | 30–70   | ASHRAE 55 |

---

## Sistema de alertas

```
Leitura recebida
      │
      ▼
AqiCalculatorService.calculate()
      │
      ▼
AlertService.analyzeReading()  ←── @Async (não bloqueia resposta ao sensor)
      │
      ├── AQI >= UNHEALTHY_SENSITIVE → criar Alert
      ├── CO₂ > 1500 ppm → criar Alert adicional
      │
      ▼
NotificationService.sendAlertNotifications()
      ├── WebSocket → dashboard em tempo real (/topic/alerts/{schoolId})
      └── E-mail → diretoria (apenas CRITICAL e EMERGENCY)

Scheduler (a cada 5 min):
      └── Sensores sem dados > 15 min → OFFLINE + alerta
```

---

## Licença

MIT — livre para uso educacional e institucional.
