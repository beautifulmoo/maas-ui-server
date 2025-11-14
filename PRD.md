# MaaS UI Server - Product Requirements Document (PRD)

## 1. 제품 개요 (Overview)

### 1.1 프로젝트 목적
MaaS UI Server는 **MAAS (Metal as a Service)** 인프라를 관리하기 위한 현대적인 웹 기반 관리 인터페이스입니다. 이 프로젝트는 MAAS 서버와의 상호작용을 단순화하고, 물리적 서버의 프로비저닝, 커미셔닝, 배포, 네트워크 설정 등의 작업을 직관적인 UI를 통해 수행할 수 있도록 합니다.

### 1.2 주요 사용자
- **인프라 엔지니어**: 물리적 서버의 라이프사이클 관리
- **운영자**: 일상적인 서버 모니터링 및 상태 확인
- **DevOps 엔지니어**: 자동화된 프로비저닝 워크플로우 관리

### 1.3 핵심 가치 및 문제 해결
- **통합 관리 인터페이스**: MAAS CLI나 직접 API 호출 없이 웹 브라우저를 통해 모든 작업 수행
- **실시간 모니터링**: WebSocket을 통한 실시간 머신 상태 업데이트
- **자동화된 워크플로우**: 커미셔닝, 네트워크 설정, 배포를 단일 인터페이스에서 처리
- **시각적 피드백**: 머신 상태, 전원 상태, 네트워크 정보를 한눈에 확인
- **설정 관리**: MAAS 서버 연결 정보 및 UI 설정을 영구 저장

---

## 2. 아키텍처 및 구성 (System Architecture)

### 2.1 디렉터리 구조

```
maas-ui-server/
├── backend/
│   └── maas-ui-backend/          # Spring Boot 백엔드 애플리케이션
│       ├── src/main/java/com/maas/ui/
│       │   ├── config/            # 설정 클래스
│       │   │   ├── CorsConfig.java
│       │   │   └── WebSocketConfig.java
│       │   ├── controller/        # REST API 컨트롤러
│       │   │   └── MaasController.java
│       │   ├── handler/           # WebSocket 핸들러
│       │   │   └── ClientWebSocketHandler.java
│       │   ├── service/           # 비즈니스 로직 서비스
│       │   │   ├── MaasApiService.java      # MAAS API 통신
│       │   │   ├── MaasAuthService.java     # OAuth 1.0 인증
│       │   │   ├── MaasWebSocketService.java # MAAS WebSocket 연결
│       │   │   └── SettingsService.java     # 설정 파일 관리
│       │   └── MaasUiBackendApplication.java
│       ├── src/main/resources/
│       │   └── application.properties
│       ├── maas-ui-settings.json  # 설정 파일 (JSON)
│       └── pom.xml
├── frontend/                      # Vue.js 3 프론트엔드
│   ├── src/
│   │   ├── components/            # Vue 컴포넌트
│   │   │   ├── DashboardTab.vue
│   │   │   ├── MachinesTab.vue
│   │   │   └── SettingsTab.vue
│   │   ├── composables/           # Vue Composition API 유틸리티
│   │   │   ├── useSettings.js
│   │   │   └── useWebSocket.js
│   │   ├── App.vue
│   │   └── main.js
│   ├── public/
│   │   └── index.html
│   ├── webpack.config.js
│   └── package.json
└── README.md
```

### 2.2 모듈 역할 및 상호작용

#### 백엔드 모듈
1. **MaasController**: REST API 엔드포인트 제공
   - 프론트엔드로부터 HTTP 요청 수신
   - MaasApiService, SettingsService 호출
   - 응답 데이터를 JSON으로 반환

2. **MaasApiService**: MAAS API와의 통신 담당
   - WebClient를 사용한 비동기 HTTP 요청
   - OAuth 1.0 인증 헤더 생성 (MaasAuthService 사용)
   - MAAS API 응답 파싱 및 에러 처리

3. **MaasAuthService**: OAuth 1.0 인증 처리
   - API 키 형식 검증 (`consumer_key:token:token_secret`)
   - OAuth 헤더 생성 (nonce, timestamp 포함)

4. **MaasWebSocketService**: MAAS WebSocket 서버 연결
   - MAAS 서버에 로그인하여 CSRF 토큰 및 세션 ID 획득
   - Jetty WebSocketClient를 사용한 WebSocket 연결
   - Heartbeat 및 재연결 로직 관리
   - MAAS로부터 받은 메시지를 ClientWebSocketHandler로 전달

5. **ClientWebSocketHandler**: 프론트엔드 WebSocket 클라이언트 관리
   - 프론트엔드와의 WebSocket 연결 관리
   - MAAS 메시지를 모든 연결된 클라이언트에게 브로드캐스트

6. **SettingsService**: 설정 파일 관리
   - `maas-ui-settings.json` 파일 읽기/쓰기
   - 기본값 제공 및 설정 검증

#### 프론트엔드 모듈
1. **App.vue**: 메인 애플리케이션 컴포넌트
   - 탭 네비게이션 (Dashboard, Machines, Settings)
   - 전역 스타일 및 레이아웃

2. **DashboardTab.vue**: 대시보드 화면
   - 머신 통계 표시 (전체, 커미셔닝됨, 배포됨)
   - 백엔드 API 호출을 통한 데이터 로드

3. **MachinesTab.vue**: 머신 관리 화면
   - 머신 목록 테이블 표시
   - 검색, 필터링, 페이지네이션
   - Commission, Network, Deploy 액션 버튼
   - WebSocket을 통한 실시간 업데이트

4. **SettingsTab.vue**: 설정 화면
   - MAAS 서버 URL 및 API 키 설정
   - 연결 테스트 기능
   - 새로고침 간격, 자동 새로고침 설정

5. **useSettings.js**: 설정 관리 Composable
   - 백엔드 API와 동기화된 설정 상태 관리
   - 설정 저장/로드/리셋 기능

6. **useWebSocket.js**: WebSocket 연결 관리 Composable
   - 백엔드 WebSocket 서버 연결
   - 자동 재연결 및 Heartbeat 관리
   - 초기 구독 메시지 전송

### 2.3 데이터 흐름

#### REST API 요청 흐름
```
프론트엔드 (Vue.js)
    ↓ HTTP Request (axios)
백엔드 Controller (MaasController)
    ↓ Service 호출
MaasApiService
    ↓ OAuth 1.0 헤더 생성 (MaasAuthService)
    ↓ WebClient HTTP Request
MAAS API 서버
    ↓ JSON Response
MaasApiService (응답 파싱)
    ↓ Mono<Map<String, Object>>
Controller (ResponseEntity)
    ↓ JSON Response
프론트엔드 (데이터 표시)
```

#### WebSocket 실시간 업데이트 흐름
```
MAAS WebSocket 서버
    ↓ WebSocket Message
MaasWebSocketService (MaasWebSocketListener)
    ↓ 메시지 수신
ClientWebSocketHandler (broadcastToClients)
    ↓ 모든 클라이언트에게 브로드캐스트
프론트엔드 WebSocket 클라이언트 (useWebSocket)
    ↓ 메시지 파싱 및 상태 업데이트
MachinesTab.vue (실시간 UI 업데이트)
```

### 2.4 외부 의존성

#### 백엔드
- **Spring Boot 3.2.0**: 웹 애플리케이션 프레임워크
- **Spring WebFlux**: 리액티브 프로그래밍 (WebClient)
- **Spring WebSocket**: WebSocket 서버 지원
- **Jetty WebSocket Client**: MAAS WebSocket 서버 연결
- **Jackson**: JSON 처리
- **Lombok**: 보일러플레이트 코드 제거

#### 프론트엔드
- **Vue.js 3**: 프론트엔드 프레임워크 (Composition API)
- **Vue Router 4**: 라우팅 (현재 미사용, 향후 확장용)
- **Vuex 4**: 상태 관리 (현재 미사용, 향후 확장용)
- **Axios**: HTTP 클라이언트
- **Webpack 5**: 번들러
- **Babel**: JavaScript 트랜스파일러

#### 외부 서비스
- **MAAS API**: REST API 및 WebSocket API
  - OAuth 1.0 인증 필요
  - API 버전: 2.0

---

## 3. 주요 기능 명세 (Functional Specification)

### 3.1 Dashboard 기능

#### 기능명: 머신 통계 대시보드
- **설명**: 전체 머신의 상태를 요약하여 카드 형태로 표시
- **입력**: 없음 (자동 로드)
- **출력**: 
  - 전체 머신 수
  - 커미셔닝된 머신 수
  - 배포된 머신 수
- **주요 코드 파일**: 
  - `DashboardTab.vue`: UI 컴포넌트
  - `MaasController.getDashboardStats()`: API 엔드포인트
  - `MaasApiService.getAllMachines()`: MAAS API 호출
  - `MaasApiService.calculateMachineStats()`: 통계 계산
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 `loadMachineStats()` 호출
  2. 백엔드 `/api/dashboard/stats` 엔드포인트 호출
  3. 백엔드가 MAAS API로부터 모든 머신 정보 조회
  4. 머신 상태별로 카운트 계산 (status 값 기반)
  5. 통계 데이터를 프론트엔드로 반환
  6. 카드 형태로 표시
- **에러 처리**: 
  - API 호출 실패 시 에러 메시지 표시
  - 목업 데이터로 폴백 (개발용)

### 3.2 Machines Tab 기능

#### 기능명: 머신 목록 조회 및 표시
- **설명**: MAAS에 등록된 모든 머신을 테이블 형태로 표시
- **입력**: 
  - 검색 쿼리 (선택)
  - 상태 필터 (All, New, Commissioning, Ready, Allocated, Deployed, Failed)
  - 페이지 번호
- **출력**: 머신 목록 테이블
  - FQDN, Power State, Status, Owner/Tags, Pool, Zone, Fabric
  - CPU Cores, RAM, Disk Count, Storage
  - 액션 버튼 (Commission, Network, Deploy)
- **주요 코드 파일**:
  - `MachinesTab.vue`: 메인 컴포넌트
  - `MaasController.getAllMachines()`: API 엔드포인트
  - `MaasApiService.getAllMachines()`: MAAS API 호출
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 초기 로드
  2. 백엔드 `/api/machines` 엔드포인트 호출
  3. MAAS API `/MAAS/api/2.0/machines/` 호출
  4. 응답 파싱 (배열 또는 `{"results": [...]}` 형태 처리)
  5. 프론트엔드에서 검색/필터링/페이지네이션 적용
  6. 테이블 렌더링
  7. WebSocket을 통한 실시간 업데이트 수신
- **에러 처리**:
  - API 호출 실패 시 에러 메시지 표시
  - 빈 결과 처리

#### 기능명: 머신 추가 (Add Machine)
- **설명**: 새로운 물리적 서버를 MAAS에 등록
- **입력**:
  - Hostname (선택)
  - Architecture (기본값: amd64)
  - MAC Addresses (필수, 쉼표로 구분)
  - Power Type (기본값: manual)
  - Commission 여부 (기본값: false)
  - Description (선택)
- **출력**: 추가된 머신 정보
- **주요 코드 파일**:
  - `MachinesTab.vue`: 모달 UI 및 폼 처리
  - `MaasController.addMachine()`: API 엔드포인트
  - `MaasApiService.addMachine()`: MAAS API 호출
- **내부 로직 흐름**:
  1. "Add Machine" 버튼 클릭 → 모달 표시
  2. 사용자가 폼 입력
  3. MAC 주소 형식 검증
  4. 백엔드 `/api/machines` POST 요청
  5. MAAS API `/MAAS/api/2.0/machines/` POST 요청
  6. 성공 시 머신 목록 새로고침
- **에러 처리**:
  - MAC 주소 형식 오류 검증
  - API 오류 메시지 표시

#### 기능명: 머신 커미셔닝 (Commission)
- **설명**: 머신을 커미셔닝하여 하드웨어 정보 수집 및 설정
- **입력**: 
  - System ID (머신 ID)
  - Skip BMC Config (기본값: 1)
- **출력**: 커미셔닝 작업 시작 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: 버튼 클릭 핸들러
  - `MaasController.commissionMachine()`: API 엔드포인트
  - `MaasApiService.commissionMachine()`: MAAS API 호출
- **내부 로직 흐름**:
  1. "Commission" 버튼 클릭
  2. 백엔드 `/api/machines/{systemId}/commission` POST 요청
  3. MAAS API `/MAAS/api/2.0/machines/{systemId}/op-commission` POST 요청
  4. 성공 시 버튼이 "Abort"로 변경
  5. WebSocket을 통한 상태 업데이트 수신
- **에러 처리**:
  - 커미셔닝 중인 머신은 버튼 비활성화
  - API 오류 메시지 표시

#### 기능명: 커미셔닝 중단 (Abort Commissioning)
- **설명**: 진행 중인 커미셔닝 작업을 중단
- **입력**: System ID
- **출력**: 중단 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: Abort 버튼 클릭 핸들러
  - `MaasController.abortCommissioning()`: API 엔드포인트
  - `MaasApiService.abortCommissioning()`: MAAS API 호출
- **내부 로직 흐름**:
  1. 커미셔닝 중인 머신의 "Abort" 버튼 클릭
  2. 백엔드 `/api/machines/{systemId}/abort` POST 요청
  3. MAAS API `/MAAS/api/2.0/machines/{systemId}/op-abort` POST 요청
  4. 성공 시 버튼이 "Commission"으로 변경
- **에러 처리**: API 오류 메시지 표시

#### 기능명: 네트워크 설정 (Network)
- **설명**: 머신의 네트워크 인터페이스에 VLAN 및 IP 주소 설정
- **입력**:
  - System ID
  - Interface ID
  - Fabric ID (선택, 변경 시)
  - VLAN ID (Fabric 변경 시 자동 설정)
  - Subnet ID
  - IP Assignment Mode (Unconfigured, Automatic, Static)
  - Primary IP Address (Static 모드일 때)
  - Secondary IP Addresses (여러 개 가능)
- **출력**: 업데이트된 인터페이스 정보
- **주요 코드 파일**:
  - `MachinesTab.vue`: 네트워크 모달 UI
  - `MaasController.updateInterfaceVlan()`: VLAN 업데이트 엔드포인트
  - `MaasController.linkSubnetToInterface()`: IP 주소 링크 엔드포인트
  - `MaasController.unlinkSubnetFromInterface()`: IP 주소 언링크 엔드포인트
  - `MaasApiService`: 관련 API 호출 메서드들
- **내부 로직 흐름**:
  1. "Network" 버튼 클릭 → 네트워크 모달 표시
  2. 머신의 인터페이스 목록 로드 (`interface_set`)
  3. Fabric, VLAN, Subnet 목록 로드
  4. Fabric-VLAN-Subnet 계층 구조 매핑 생성
  5. 사용자가 Fabric 선택 (선택 시 해당 Fabric의 VLAN 목록 필터링)
  6. IP Assignment Mode 선택:
     - **Unconfigured**: IP 주소 미설정
     - **Automatic**: DHCP를 통한 자동 할당
     - **Static**: 수동 IP 주소 입력
  7. Static 모드일 때:
     - Primary IP 주소 입력 및 검증
     - Secondary IP 주소 추가/제거 (여러 개 가능)
     - 각 IP 주소는 Subnet 선택 필수
  8. "Save" 버튼 클릭
  9. Fabric 변경 시 VLAN 업데이트 (`PUT /api/machines/{systemId}/interfaces/{interfaceId}/vlan`)
  10. 기존 IP 링크 언링크 (필요시)
  11. Primary IP 링크 (`POST /api/machines/{systemId}/interfaces/{interfaceId}/link-subnet`)
  12. Secondary IP 링크 (각각에 대해)
  13. 성공 시 인터페이스 정보 새로고침
- **에러 처리**:
  - 인터페이스 로드 실패 처리
  - Fabric-VLAN-Subnet 계층 구조 검증
  - IP 주소 형식 검증 (IPv4)
  - Subnet CIDR 범위 내 IP 주소 검증
  - API 오류 메시지 표시
- **특수 기능**:
  - **Fabric-VLAN-Subnet 계층 구조**: Fabric 선택 시 해당 Fabric에 속한 VLAN과 Subnet만 표시
  - **Secondary IP 관리**: 여러 개의 Secondary IP 주소를 동적으로 추가/제거 가능
  - **IP 주소 자동 완성**: Subnet CIDR 기반으로 네트워크 프리픽스 자동 제안

#### 기능명: 머신 배포 (Deploy)
- **설명**: 커미셔닝된 머신에 OS를 배포
- **입력**: System ID
- **출력**: 배포 작업 시작 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: Deploy 버튼 클릭 핸들러
  - `MaasController.deployMachine()`: API 엔드포인트
  - `MaasApiService.deployMachine()`: MAAS API 호출
- **내부 로직 흐름**:
  1. "Deploy" 버튼 클릭 (Ready 또는 Allocated 상태에서만 활성화)
  2. 백엔드 `/api/machines/{systemId}/deploy` POST 요청
  3. MAAS API `/MAAS/api/2.0/machines/{systemId}/op-deploy` POST 요청
  4. 성공 시 버튼이 "Deployed"로 변경
  5. WebSocket을 통한 상태 업데이트 수신
- **에러 처리**:
  - 배포 불가능한 상태에서는 버튼 비활성화
  - API 오류 메시지 표시

#### 기능명: 머신 릴리스 (Release)
- **설명**: 배포 실패한 머신을 릴리스하여 다시 사용 가능한 상태로 변경
- **입력**: System ID
- **출력**: 릴리스 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: Release 버튼 클릭 핸들러
  - `MaasController.releaseMachine()`: API 엔드포인트
  - `MaasApiService.releaseMachine()`: MAAS API 호출
- **내부 로직 흐름**:
  1. Failed Deployment 또는 Failed Disk Erasing 상태의 머신에서 "Release" 버튼 클릭
  2. 백엔드 `/api/machines/{systemId}/release` POST 요청
  3. MAAS API `/MAAS/api/2.0/machines/{systemId}/op-release` POST 요청 (force=true)
  4. 성공 시 머신 상태가 "New"로 변경
- **에러 처리**: API 오류 메시지 표시

#### 기능명: Block Devices 조회
- **설명**: 머신의 블록 디바이스(디스크) 정보 조회 및 스토리지 계산
- **입력**: System ID
- **출력**: Block devices 목록 및 총 스토리지 크기
- **주요 코드 파일**:
  - `MachinesTab.vue`: Block device 정보 처리 및 스토리지 계산
  - `MaasController.getMachineBlockDevices()`: API 엔드포인트
  - `MaasApiService.getMachineBlockDevices()`: MAAS API 호출
  - `calculateStorage()`: 스토리지 크기 계산 함수
- **내부 로직 흐름**:
  1. 머신 목록 로드 시 각 머신의 상세 정보 병렬 조회
  2. 백엔드 `/api/machines/{systemId}` GET 요청 (blockdevice_set 포함)
  3. 또는 `/api/machines/{systemId}/block-devices` GET 요청
  4. MAAS API `/MAAS/api/2.0/machines/{systemId}/block_devices/` 호출
  5. Block device 목록에서 디스크 수 계산 (`blockdevice_set.length`)
  6. 각 block device의 `size` 값을 합산하여 총 스토리지 계산
  7. UI에 디스크 수 및 스토리지 크기 표시 (GB/MiB 단위)
- **에러 처리**:
  - Block device 정보 조회 실패 시 기본값 사용 (0 disks, 0 storage)
  - 개별 머신 정보 조회 실패해도 다른 머신 처리 계속 진행
- **성능 최적화**:
  - 모든 머신의 상세 정보를 `Promise.all()`로 병렬 조회
  - 대량 머신 처리 시에도 효율적인 데이터 로딩

### 3.3 Settings Tab 기능

#### 기능명: MAAS 서버 설정
- **설명**: MAAS 서버 URL 및 API 키 설정
- **입력**:
  - MAAS Server URL (예: `http://192.168.189.71:5240`)
  - API Key (OAuth 1.0 형식: `consumer_key:token:token_secret`)
- **출력**: 설정 저장 성공/실패
- **주요 코드 파일**:
  - `SettingsTab.vue`: 설정 UI
  - `useSettings.js`: 설정 상태 관리
  - `MaasController.getSettings()` / `saveSettings()`: API 엔드포인트
  - `SettingsService`: 설정 파일 I/O
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 백엔드에서 설정 로드
  2. 사용자가 URL 및 API 키 입력
  3. "Save Settings" 버튼 클릭
  4. 백엔드 `/api/settings` POST 요청
  5. `maas-ui-settings.json` 파일에 저장
  6. 성공 메시지 표시
- **에러 처리**:
  - 설정 파일 저장 실패 처리
  - 기본값으로 폴백

#### 기능명: 연결 테스트
- **설명**: MAAS 서버 연결 및 인증 테스트
- **입력**: MAAS URL, API Key
- **출력**: 연결 성공/실패 메시지
- **주요 코드 파일**:
  - `SettingsTab.vue`: 테스트 버튼
  - `MaasController.testConnection()`: API 엔드포인트
  - `MaasApiService.testConnection()`: MAAS API `/MAAS/api/2.0/version/` 호출
- **내부 로직 흐름**:
  1. "Test Connection" 버튼 클릭
  2. 백엔드 `/api/test-connection` GET 요청
  3. MAAS API `/MAAS/api/2.0/version/` 호출 (인증 테스트)
  4. 성공 시 "Connection successful" 메시지 표시
  5. 실패 시 에러 메시지 표시
- **에러 처리**:
  - 네트워크 오류 처리
  - 인증 실패 처리
  - 타임아웃 처리 (10초)

#### 기능명: 새로고침 설정
- **설명**: 자동 새로고침 간격 및 활성화 여부 설정
- **입력**:
  - Auto Refresh Interval (초, 10-300)
  - Enable Auto Refresh (체크박스)
- **출력**: 설정 저장 성공/실패
- **내부 로직**: Settings 기능과 동일한 흐름

#### 기능명: 표시 설정
- **설명**: UI 표시 옵션 설정
- **입력**:
  - Items per page (10, 25, 50, 100)
  - Show Advanced Machine Information (체크박스)
- **출력**: 설정 저장 성공/실패
- **내부 로직**: Settings 기능과 동일한 흐름

### 3.4 머신 상태별 액션 버튼 동작

시스템은 머신의 현재 상태에 따라 Commission, Network, Deploy 버튼의 색상, 기능, 활성화 여부가 동적으로 변경됩니다.

#### Commission 버튼 동작

| 머신 상태 | 버튼 색상 | 버튼 텍스트 | 활성화 여부 | 기능 |
|---------|---------|-----------|-----------|------|
| **New** | 녹색 (`btn-success`) | "Commission" | 활성화 | 커미셔닝 시작 |
| **Commissioning** | 주황색 (`btn-warning`) | "Abort" | 중단 중이면 비활성화 | 커미셔닝 중단 |
| **Ready** | 연한 녹색 (`btn-success-light`) | "Commission" | 활성화 | 재커미셔닝 (확인 메시지 표시) |
| **Allocated** | 연한 녹색 (`btn-success-light`) | "Commission" | 활성화 | 재커미셔닝 (확인 메시지 표시) |
| **Deployed** | 연한 녹색 (`btn-success-light`) | "Commission" | 활성화 | 재커미셔닝 (확인 메시지 표시) |
| **Failed** (deployment 제외) | 녹색 (`btn-success`) | "Commission" | 활성화 | 커미셔닝 재시도 |
| **Failed Deployment** | - | - | - | Release 버튼 표시 (Commission 버튼 대신) |
| **Failed Disk Erasing** | - | - | - | Release 버튼 표시 (Commission 버튼 대신) |
| **Deploying** | 회색 (`btn-secondary`) | "Commission" | 비활성화 | 사용 불가 |
| **Disk Erasing** | 회색 (`btn-secondary`) | "Commission" | 비활성화 | 사용 불가 |

**특수 동작**:
- **재커미셔닝**: Ready, Allocated, Deployed 상태에서 Commission 버튼 클릭 시 확인 메시지 표시
  - "이 머신은 이미 Commissioning이 완료되어 {상태} 상태입니다. 정말로 다시 Commissioning을 진행하시겠습니까?"
- **Abort 중**: Commissioning 중 Abort 버튼 클릭 시, 중단 요청 처리 중에는 버튼이 비활성화되고 "..." 표시

#### Network 버튼 동작

| 머신 상태 | 버튼 색상 | 활성화 여부 | 네트워크 설정 저장 가능 여부 | 기능 |
|---------|---------|-----------|----------------------|------|
| **New** | 연한 파랑 (`btn-primary-light`) | 활성화 | ❌ 불가 | 네트워크 모달 열기 (읽기 전용) |
| **Ready** | 파랑 (`btn-primary`) | 활성화 | ✅ 가능 | 네트워크 설정 변경 및 저장 |
| **Allocated** | 파랑 (`btn-primary`) | 활성화 | ✅ 가능 | 네트워크 설정 변경 및 저장 |
| **Deployed** | 연한 파랑 (`btn-primary-light`) | 활성화 | ❌ 불가 | 네트워크 모달 열기 (읽기 전용) |
| **Failed** (모든 종류) | 연한 파랑 (`btn-primary-light`) | 활성화 | ❌ 불가 | 네트워크 모달 열기 (읽기 전용) |
| **Commissioning** | 회색 (`btn-secondary`) | 비활성화 | - | 사용 불가 |
| **Deploying** | 회색 (`btn-secondary`) | 비활성화 | - | 사용 불가 |
| **Disk Erasing** | 회색 (`btn-secondary`) | 비활성화 | - | 사용 불가 |

**네트워크 설정 저장 규칙**:
- **저장 가능 상태**: Ready, Allocated 상태에서만 네트워크 변경사항 저장 가능
- **저장 불가 상태**: New, Deployed, Failed 상태에서는 모달은 열리지만 "Save Changes" 버튼이 비활성화됨
- **진행 중 상태**: Commissioning, Deploying, Disk Erasing 상태에서는 버튼 자체가 비활성화

#### Deploy 버튼 동작

| 머신 상태 | 버튼 색상 | 버튼 텍스트 | 활성화 여부 | 기능 |
|---------|---------|-----------|-----------|------|
| **Ready** | 분홍색 (`btn-deploy`) | "Deploy" | 활성화 | 배포 시작 |
| **Allocated** | 분홍색 (`btn-deploy`) | "Deploy" | 활성화 | 배포 시작 |
| **Deploying** | 주황색 (`btn-warning`) | "Abort" | 중단 중이면 비활성화 | 배포 중단 |
| **Deployed** | 회색 (`btn-secondary`) | "Deployed" | 비활성화 | 사용 불가 |
| **New** | 회색 (`btn-secondary`) | "Deploy" | 비활성화 | 사용 불가 |
| **Commissioning** | 회색 (`btn-secondary`) | "Deploy" | 비활성화 | 사용 불가 |
| **Failed** | 회색 (`btn-secondary`) | "Deploy" | 비활성화 | 사용 불가 |

**특수 동작**:
- **Abort 중**: Deploying 중 Abort 버튼 클릭 시, 중단 요청 처리 중에는 버튼이 비활성화되고 "..." 표시
- **배포 시작**: Ready 또는 Allocated 상태에서만 배포 가능

#### Release 버튼 동작

| 머신 상태 | 버튼 표시 | 버튼 색상 | 활성화 여부 | 기능 |
|---------|---------|---------|-----------|------|
| **Failed Deployment** | 표시 | Release 스타일 (`btn-release`) | 릴리스 중이면 비활성화 | 머신 릴리스 (force=true) |
| **Failed Disk Erasing** | 표시 | Release 스타일 (`btn-release`) | 릴리스 중이면 비활성화 | 머신 릴리스 (force=true) |
| **그 외 모든 상태** | 숨김 | - | - | Commission 버튼이 표시됨 |

**특수 동작**:
- Failed Deployment 또는 Failed Disk Erasing 상태에서만 Release 버튼이 Commission 버튼 대신 표시됨
- Release 후 머신 상태는 "New"로 변경됨

#### 버튼 상태 전환 흐름

```
New
  ↓ [Commission 클릭]
Commissioning
  ↓ [완료] 또는 [Abort 클릭]
Ready/Allocated
  ↓ [Deploy 클릭]
Deploying
  ↓ [완료] 또는 [Abort 클릭]
Deployed

Failed Deployment/Failed Disk Erasing
  ↓ [Release 클릭]
New (다시 시작)
```

#### 버튼 색상 코드

- **`btn-success`**: 녹색 - 일반적인 Commission 버튼 (New, Failed 상태)
- **`btn-success-light`**: 연한 녹색 - 재커미셔닝 가능 상태 (Ready, Allocated, Deployed)
- **`btn-warning`**: 주황색 - 중단 버튼 (Commissioning 중 Abort, Deploying 중 Abort)
- **`btn-primary`**: 파랑 - 활성화된 Network 버튼 (Ready, Allocated)
- **`btn-primary-light`**: 연한 파랑 - 읽기 전용 Network 버튼 (New, Deployed, Failed)
- **`btn-deploy`**: 분홍색 - 활성화된 Deploy 버튼 (Ready, Allocated)
- **`btn-secondary`**: 회색 - 비활성화된 버튼
- **`btn-release`**: Release 전용 스타일 - Failed 상태에서 릴리스용

#### 주요 코드 파일

- `MachinesTab.vue`:
  - `getCommissionButtonClass()`: Commission 버튼 색상 결정
  - `getCommissionButtonDisabled()`: Commission 버튼 활성화 여부 결정
  - `handleCommissionButtonClick()`: Commission 버튼 클릭 처리
  - `getNetworkButtonClass()`: Network 버튼 색상 결정
  - `getNetworkButtonDisabled()`: Network 버튼 활성화 여부 결정
  - `canSaveNetworkChanges()`: 네트워크 설정 저장 가능 여부 확인
  - `getDeployButtonClass()`: Deploy 버튼 색상 결정
  - `getDeployButtonDisabled()`: Deploy 버튼 활성화 여부 결정
  - `handleDeployButtonClick()`: Deploy 버튼 클릭 처리
  - `isFailedDeployment()`: Failed Deployment 상태 확인

### 3.5 WebSocket 실시간 업데이트

#### 기능명: 실시간 머신 상태 업데이트
- **설명**: MAAS 서버로부터 실시간으로 머신 상태 변경 사항 수신
- **입력**: 없음 (자동 연결)
- **출력**: WebSocket 메시지 (JSON)
- **주요 코드 파일**:
  - `useWebSocket.js`: 프론트엔드 WebSocket 클라이언트
  - `ClientWebSocketHandler.java`: 백엔드 WebSocket 핸들러
  - `MaasWebSocketService.java`: MAAS WebSocket 서버 연결
- **내부 로직 흐름**:
  1. 프론트엔드 컴포넌트 마운트 시 WebSocket 연결 시도
  2. 백엔드 `/ws` 엔드포인트로 연결
  3. 백엔드가 MAAS WebSocket 서버에 연결 (아직 연결되지 않은 경우)
  4. MAAS 서버에 로그인하여 CSRF 토큰 및 세션 ID 획득
  5. 초기 구독 메시지 전송:
     - `user.auth_user`
     - `notification.list`
     - `resourcepool.list`
     - `machine.list` (필터, 페이지네이션, 정렬 포함)
  6. MAAS로부터 메시지 수신 시 모든 프론트엔드 클라이언트에게 브로드캐스트
  7. 프론트엔드에서 메시지 파싱 및 머신 상태 업데이트
  8. 재연결 시 자동으로 구독 메시지 재전송
- **메시지 처리 로직**:
  - **재연결 알림**: `{"type":"reconnect"}` 메시지 수신 시 구독 메시지 재전송
  - **머신 상태 업데이트**: `machine.list` 응답에서 머신 정보 추출 및 UI 업데이트
  - **상태 동기화**: WebSocket 메시지와 REST API 응답 간 일관성 유지
- **에러 처리**:
  - 연결 실패 시 자동 재연결 (5초 간격)
  - Heartbeat를 통한 연결 상태 모니터링 (60초 간격, 실제 ping은 비활성화)
  - Idle timeout 처리 (10분)
  - 특정 에러 코드 (1000, 1003, 1007)는 재연결하지 않음
  - 재연결 시 초기 구독 메시지 자동 재전송 (2초 지연)

---

## 4. 기술 스펙 (Technical Specification)

### 4.1 백엔드 기술 스택

#### 프레임워크 및 라이브러리
- **Spring Boot 3.2.0**: 메인 프레임워크
- **Java 17**: 프로그래밍 언어
- **Spring WebFlux**: 리액티브 프로그래밍
  - `WebClient`: 비동기 HTTP 클라이언트
  - `Mono<T>`: 단일 값 비동기 스트림
- **Spring WebSocket**: WebSocket 서버 지원
- **Jetty WebSocket Client 11.0.18**: MAAS WebSocket 서버 연결
- **Jackson**: JSON 직렬화/역직렬화
- **Lombok**: 보일러플레이트 코드 제거

#### 주요 클래스 구조

##### MaasController
```java
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MaasController {
    // REST API 엔드포인트들
    - GET /api/health
    - GET /api/settings
    - POST /api/settings
    - GET /api/dashboard/stats
    - GET /api/machines
    - GET /api/machines/{systemId}
    - POST /api/machines
    - POST /api/machines/{systemId}/commission
    - POST /api/machines/{systemId}/abort
    - POST /api/machines/{systemId}/deploy
    - POST /api/machines/{systemId}/release
    - GET /api/machines/{systemId}/block-devices
    - GET /api/fabrics
    - GET /api/fabrics/{fabricId}/vlans
    - GET /api/subnets
    - PUT /api/machines/{systemId}/interfaces/{interfaceId}/vlan
    - POST /api/machines/{systemId}/interfaces/{interfaceId}/link-subnet
    - POST /api/machines/{systemId}/interfaces/{interfaceId}/unlink-subnet
    - GET /api/test-connection
}
```

**주요 엔드포인트 상세**:
- **네트워크 관련**: Fabric, VLAN, Subnet의 계층적 구조 관리
  - Fabric → VLAN → Subnet 순서로 필터링
  - 인터페이스에 여러 IP 주소 할당 가능 (Primary + Secondary)
- **Block Devices**: 머신의 물리적 디스크 정보 조회
  - 디스크 수, 총 스토리지 크기 계산
  - 각 디바이스의 size 속성 합산

##### MaasApiService
- **역할**: MAAS API와의 모든 통신 담당
- **주요 메서드**:
  - `getAllMachines()`: 모든 머신 조회
  - `getMachine()`: 개별 머신 조회
  - `addMachine()`: 머신 추가
  - `commissionMachine()`: 커미셔닝
  - `abortCommissioning()`: 커미셔닝 중단
  - `deployMachine()`: 배포
  - `releaseMachine()`: 릴리스
  - `getAllFabrics()`: Fabric 목록 조회
  - `getFabricVlans()`: VLAN 목록 조회
  - `getAllSubnets()`: Subnet 목록 조회
  - `updateInterfaceVlan()`: 인터페이스 VLAN 업데이트
  - `linkSubnetToInterface()`: IP 주소 링크 (Primary 및 Secondary IP 지원)
  - `unlinkSubnetFromInterface()`: IP 주소 언링크
  - `testConnection()`: 연결 테스트
  - `calculateMachineStats()`: 통계 계산
  - `getMachineBlockDevices()`: Block devices 조회

##### MaasAuthService
- **역할**: OAuth 1.0 인증 헤더 생성
- **주요 메서드**:
  - `generateAuthHeader(apiKey)`: OAuth 헤더 생성
  - `isValidApiKey(apiKey)`: API 키 형식 검증

##### MaasWebSocketService
- **역할**: MAAS WebSocket 서버 연결 및 메시지 중계
- **주요 메서드**:
  - `connectToMaas(callback)`: MAAS WebSocket 서버 연결
  - `sendToMaas(message)`: MAAS로 메시지 전송
  - `isConnected()`: 연결 상태 확인
  - `performLogin()`: MAAS 서버 로그인
  - `startHeartbeat()`: Heartbeat 스케줄러 시작
  - `reconnectToMaas()`: 재연결 로직

##### SettingsService
- **역할**: 설정 파일 관리
- **주요 메서드**:
  - `loadSettings()`: 설정 파일 로드
  - `saveSettings(settings)`: 설정 파일 저장
  - `getDefaultSettings()`: 기본값 반환

#### REST API 엔드포인트 명세

##### GET /api/health
- **설명**: 헬스 체크
- **요청**: 없음
- **응답**:
```json
{
  "status": "UP",
  "service": "MAAS UI Backend"
}
```

##### GET /api/settings
- **설명**: 설정 조회
- **요청**: 없음
- **응답**:
```json
{
  "maasUrl": "http://192.168.189.71:5240",
  "apiKey": "consumer_key:token:token_secret",
  "refreshInterval": 30,
  "autoRefresh": true,
  "itemsPerPage": "50",
  "showAdvancedInfo": false
}
```

##### POST /api/settings
- **설명**: 설정 저장
- **요청 Body**:
```json
{
  "maasUrl": "http://192.168.189.71:5240",
  "apiKey": "consumer_key:token:token_secret",
  "refreshInterval": 30,
  "autoRefresh": true,
  "itemsPerPage": "50",
  "showAdvancedInfo": false
}
```
- **응답**:
```json
{
  "success": true,
  "message": "Settings saved successfully"
}
```

##### GET /api/dashboard/stats
- **설명**: 머신 통계 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "totalMachines": 12,
  "commissionedMachines": 8,
  "deployedMachines": 5
}
```

##### GET /api/machines
- **설명**: 모든 머신 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "id": "abc123",
      "hostname": "machine-01",
      "status": 4,
      "status_name": "Ready",
      "power_state": "on",
      "cpu_count": 8,
      "memory": 16384,
      "disk_count": 2,
      "storage": 1000,
      "owner": "admin",
      "tags": ["tag1", "tag2"],
      "pool": "default",
      "zone": "default",
      "fabric": "fabric-0",
      "mac_addresses": ["aa:bb:cc:dd:ee:ff"],
      "ip_addresses": ["192.168.1.10"]
    }
  ],
  "count": 12
}
```

##### POST /api/machines
- **설명**: 머신 추가
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `hostname` (선택): 호스트명
  - `architecture` (선택, 기본값: amd64): 아키텍처
  - `macAddresses` (필수): MAC 주소 (쉼표로 구분)
  - `powerType` (선택, 기본값: manual): 전원 타입
  - `commission` (선택, 기본값: false): 커미셔닝 여부
  - `description` (선택): 설명
- **응답**:
```json
{
  "success": true,
  "data": { /* 머신 정보 */ }
}
```

##### POST /api/machines/{systemId}/commission
- **설명**: 머신 커미셔닝
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `skipBmcConfig` (선택, 기본값: 1): BMC 설정 건너뛰기
- **응답**:
```json
{
  "success": true,
  "data": { /* 커미셔닝 결과 */ }
}
```

##### POST /api/machines/{systemId}/deploy
- **설명**: 머신 배포
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "success": true,
  "data": { /* 배포 결과 */ }
}
```

##### GET /api/test-connection
- **설명**: MAAS 서버 연결 테스트
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "success": true,
  "message": "Connection successful! MAAS server is reachable."
}
```

##### GET /api/machines/{systemId}/block-devices
- **설명**: 머신의 블록 디바이스(디스크) 정보 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "id": 123,
      "name": "sda",
      "size": 1000000000000,
      "type": "physical",
      "block_size": 512,
      "tags": []
    }
  ],
  "count": 2
}
```

##### GET /api/fabrics
- **설명**: 모든 Fabric 목록 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "id": 0,
      "name": "fabric-0",
      "class_type": "fabric",
      "vlan_ids": [5001, 5002]
    }
  ],
  "count": 1
}
```

##### GET /api/fabrics/{fabricId}/vlans
- **설명**: 특정 Fabric의 VLAN 목록 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "id": 5001,
      "name": "untagged",
      "fabric_id": 0,
      "vid": 0
    }
  ],
  "count": 1
}
```

##### GET /api/subnets
- **설명**: 모든 Subnet 목록 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "id": 1,
      "name": "192.168.1.0/24",
      "cidr": "192.168.1.0/24",
      "vlan_id": 5001,
      "gateway_ip": "192.168.1.1"
    }
  ],
  "count": 1
}
```

##### PUT /api/machines/{systemId}/interfaces/{interfaceId}/vlan
- **설명**: 인터페이스의 VLAN 업데이트
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `vlanId` (필수): VLAN ID
- **응답**:
```json
{
  "success": true,
  "data": { /* 업데이트된 인터페이스 정보 */ }
}
```

##### POST /api/machines/{systemId}/interfaces/{interfaceId}/link-subnet
- **설명**: 인터페이스에 IP 주소 링크 (Primary 또는 Secondary)
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `subnetId` (필수): Subnet ID
  - `ipAddress` (선택): IP 주소 (null이면 AUTO 모드)
- **응답**:
```json
{
  "success": true,
  "data": { /* 링크된 IP 주소 정보 */ }
}
```

##### POST /api/machines/{systemId}/interfaces/{interfaceId}/unlink-subnet
- **설명**: 인터페이스에서 IP 주소 링크 삭제
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `linkId` (필수): 링크 ID
- **응답**:
```json
{
  "success": true,
  "data": { /* 언링크 결과 */ }
}
```

### 4.2 프론트엔드 기술 스택

#### 프레임워크 및 라이브러리
- **Vue.js 3.5.22**: 프론트엔드 프레임워크
  - Composition API 사용
  - `<script setup>` 구문 미사용 (명시적 setup 함수 사용)
- **Vue Router 4.6.3**: 라우팅 (현재 미사용)
- **Vuex 4.1.0**: 상태 관리 (현재 미사용)
- **Axios 1.6.0**: HTTP 클라이언트
- **Webpack 5**: 번들러
- **Babel**: JavaScript 트랜스파일러

#### 주요 컴포넌트 구조

##### App.vue
- **역할**: 메인 애플리케이션 컴포넌트
- **기능**:
  - 탭 네비게이션 (Dashboard, Machines, Settings)
  - 전역 스타일 및 레이아웃
- **상태**: `activeTab` (현재 활성 탭)

##### DashboardTab.vue
- **역할**: 대시보드 화면
- **상태**:
  - `totalMachines`: 전체 머신 수
  - `commissionedMachines`: 커미셔닝된 머신 수
  - `deployedMachines`: 배포된 머신 수
  - `loading`: 로딩 상태
  - `error`: 에러 메시지
- **메서드**:
  - `loadMachineStats()`: 통계 데이터 로드

##### MachinesTab.vue
- **역할**: 머신 관리 화면
- **상태**:
  - `machines`: 머신 목록
  - `loading`: 로딩 상태
  - `error`: 에러 메시지
  - `searchQuery`: 검색 쿼리
  - `selectedStatus`: 선택된 상태 필터
  - `selectedMachines`: 선택된 머신 ID 목록
  - `currentPage`: 현재 페이지
  - `connectionStatus`: WebSocket 연결 상태
- **메서드**:
  - `loadMachines()`: 머신 목록 로드
  - `showAddMachineModal()`: 머신 추가 모달 표시
  - `addMachine()`: 머신 추가
  - `handleCommissionButtonClick()`: 커미셔닝 버튼 클릭 처리
  - `commissionMachine()`: 머신 커미셔닝
  - `abortCommissioning()`: 커미셔닝 중단
  - `showNetworkModal()`: 네트워크 모달 표시
  - `handleDeployButtonClick()`: 배포 버튼 클릭 처리
  - `deployMachine()`: 머신 배포
  - `releaseMachine()`: 머신 릴리스
  - `filteredMachines`: 검색/필터링된 머신 목록 (computed)
  - `paginatedMachines`: 페이지네이션된 머신 목록 (computed)

##### SettingsTab.vue
- **역할**: 설정 화면
- **상태**:
  - `testing`: 연결 테스트 중 여부
  - `connectionStatus`: 연결 테스트 결과
  - `settings`: 설정 객체 (useSettings에서 가져옴)
- **메서드**:
  - `saveSettings()`: 설정 저장
  - `resetSettings()`: 설정 리셋
  - `testConnection()`: 연결 테스트

#### Composable 함수

##### useSettings.js
- **역할**: 설정 상태 관리
- **반환값**:
  - `settings`: 반응형 설정 객체
  - `isValid`: 설정 유효성 (computed)
  - `getApiParams`: API 호출 파라미터 (computed)
  - `save()`: 설정 저장
  - `refresh()`: 설정 새로고침
  - `reset()`: 설정 리셋

##### useWebSocket.js
- **역할**: WebSocket 연결 관리
- **반환값**:
  - `socket`: WebSocket 인스턴스
  - `connectionStatus`: 연결 상태 ('disconnected', 'connecting', 'connected', 'error')
  - `lastMessage`: 마지막 수신 메시지
  - `sendMessage()`: 메시지 전송

### 4.3 통신 구조

#### REST API 통신
- **프로토콜**: HTTP/HTTPS
- **인증**: OAuth 1.0 (Authorization 헤더)
- **데이터 형식**: JSON
- **에러 처리**: HTTP 상태 코드 및 JSON 에러 메시지

#### WebSocket 통신
- **프로토콜**: WebSocket (ws:// 또는 wss://)
- **백엔드 엔드포인트**: `/ws`
- **인증**: MAAS 서버 로그인 (CSRF 토큰 및 세션 ID)
- **메시지 형식**: JSON
- **재연결**: 자동 재연결 (5초 간격)
- **Heartbeat**: 연결 상태 모니터링 (60초 간격)

### 4.4 설정 파일 구조

#### maas-ui-settings.json
```json
{
  "maasUrl": "http://192.168.189.71:5240",
  "apiKey": "consumer_key:token:token_secret",
  "refreshInterval": 30,
  "autoRefresh": true,
  "itemsPerPage": "50",
  "showAdvancedInfo": false
}
```

#### application.properties
```properties
server.port=8081
spring.application.name=maas-ui-backend
logging.level.com.maas.ui=DEBUG
spring.web.cors.allowed-origins=*
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
maas.default.url=http://192.168.189.71:5240
maas.default.api-key=consumer_key:token:token_secret
```

---

## 5. UI 및 UX 요소 (UI/UX Elements)

### 5.1 페이지별 역할 및 컴포넌트 구성

#### Dashboard Tab
- **레이아웃**: 카드 그리드 (3열)
- **컴포넌트**:
  - Stat Card (전체 머신 수)
  - Stat Card (커미셔닝된 머신 수)
  - Stat Card (배포된 머신 수)
- **스타일**: 그라데이션 배경, 호버 효과

#### Machines Tab
- **레이아웃**: 테이블 형태
- **컴포넌트**:
  - 헤더 (제목, 연결 상태 표시기)
  - 검색 박스
  - 필터 버튼 (All, New, Commissioning, Ready, Allocated, Deployed, Failed)
  - "Add Machine" 버튼
  - 머신 테이블
    - 체크박스 컬럼
    - FQDN 컬럼 (호스트명, MAC 주소, IP 주소)
    - Power 컬럼
    - Status 컬럼 (상태 배지, 상태 메시지)
    - Owner/Tags 컬럼
    - Pool, Zone, Fabric 컬럼
    - 하드웨어 정보 컬럼 (Cores, RAM, Disks, Storage)
    - Actions 컬럼 (상태별 동적 버튼: Commission/Release, Network, Deploy)
  - 페이지네이션
  - 모달:
    - Add Machine 모달
    - Network 설정 모달
- **스타일**: 
  - 상태별 색상 코딩 (New: 파랑, Commissioning: 노랑, Ready: 초록, Deployed: 회색, Failed: 빨강)
  - 버튼 상태별 색상:
    - **Commission**: 녹색/연한 녹색 (활성화), 주황색 (Abort), 회색 (비활성화)
    - **Network**: 파랑 (저장 가능), 연한 파랑 (읽기 전용), 회색 (비활성화)
    - **Deploy**: 분홍색 (활성화), 주황색 (Abort), 회색 (비활성화)
    - **Release**: Release 전용 스타일 (Failed 상태)

#### Settings Tab
- **레이아웃**: 섹션별 폼
- **컴포넌트**:
  - MAAS API Configuration 섹션
    - MAAS Server URL 입력 필드
    - API Key 입력 필드
    - "Test Connection" 버튼
    - 연결 상태 메시지
  - Refresh Settings 섹션
    - Auto Refresh Interval 입력 필드
    - Enable Auto Refresh 체크박스
  - Display Settings 섹션
    - Items per page 선택 필드
    - Show Advanced Machine Information 체크박스
  - 액션 버튼:
    - "Save Settings" 버튼
    - "Reset to Defaults" 버튼
- **스타일**: 카드 형태의 섹션, 폼 입력 필드, 버튼

### 5.2 사용자 입력 흐름

#### 머신 추가 흐름
1. Machines Tab에서 "Add Machine" 버튼 클릭
2. 모달 표시
3. 폼 입력 (Hostname, MAC Addresses, Architecture, Power Type, Commission 여부, Description)
4. MAC 주소 형식 검증
5. "Add" 버튼 클릭
6. API 호출
7. 성공 시 모달 닫기 및 머신 목록 새로고침

#### 커미셔닝 흐름
1. Machines Tab에서 머신의 "Commission" 버튼 클릭
2. API 호출
3. 버튼이 "Abort"로 변경
4. WebSocket을 통한 상태 업데이트 수신
5. 커미셔닝 완료 시 버튼이 다시 "Commission"으로 변경

#### 네트워크 설정 흐름
1. Machines Tab에서 머신의 "Network" 버튼 클릭
2. 네트워크 모달 표시
3. 인터페이스 목록 로드
4. Fabric, VLAN, Subnet 목록 로드
5. 사용자가 VLAN 및 Subnet 선택
6. IP 주소 입력 (선택)
7. "Save" 버튼 클릭
8. API 호출 (VLAN 업데이트, Subnet 링크)
9. 성공 시 인터페이스 정보 새로고침

#### 배포 흐름
1. Machines Tab에서 Ready/Allocated 상태 머신의 "Deploy" 버튼 클릭
2. API 호출
3. 버튼이 "Deployed"로 변경
4. WebSocket을 통한 상태 업데이트 수신

### 5.3 CSS/스타일 프레임워크
- **프레임워크**: 커스텀 CSS (프레임워크 미사용)
- **스타일 방식**: Scoped CSS (Vue 컴포넌트별)
- **색상 팔레트**:
  - Primary: #007bff (파랑)
  - Secondary: #6c757d (회색)
  - Success: #28a745 (초록)
  - Warning: #ffc107 (노랑)
  - Danger: #dc3545 (빨강)
  - Background: #f5f5f5 (연한 회색)
  - Card Background: #ffffff (흰색)

### 5.4 주요 시각적 규칙

#### 상태 표시
- **New**: 파랑 배지
- **Commissioning**: 노랑 배지
- **Ready**: 초록 배지
- **Allocated**: 초록 배지
- **Deployed**: 회색 배지 (비활성화)
- **Failed**: 빨강 배지

#### 버튼 상태
- **활성화**: 파랑 배경 (#007bff)
- **비활성화**: 회색 배경 (#6c757d), cursor: not-allowed
- **경고 (Abort)**: 주황 배경
- **로딩 중**: "..." 텍스트 표시

#### 연결 상태 표시기
- **Live (connected)**: 초록색
- **Connecting**: 노랑색
- **Error**: 빨강색
- **Offline (disconnected)**: 회색

---

## 6. 배포 및 실행 환경

### 6.1 실행 방법

#### 백엔드 실행
```bash
cd backend/maas-ui-backend
mvn clean install
mvn spring-boot:run
```
- **포트**: 8081
- **접속 URL**: http://localhost:8081

#### 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```
- **포트**: 8080
- **접속 URL**: http://localhost:8080

#### 프로덕션 빌드
```bash
# 프론트엔드 빌드
cd frontend
npm run build

# 백엔드 빌드
cd backend/maas-ui-backend
mvn clean package
java -jar target/maas-ui-backend-1.0-SNAPSHOT.jar
```

### 6.2 환경 변수 및 설정 항목

#### 백엔드 설정 (application.properties)
- `server.port`: 서버 포트 (기본값: 8081)
- `maas.default.url`: 기본 MAAS 서버 URL
- `maas.default.api-key`: 기본 API 키
- `logging.level.com.maas.ui`: 로깅 레벨 (DEBUG, INFO, WARN, ERROR)

#### 프론트엔드 설정
- 설정은 런타임에 `maas-ui-settings.json` 파일에서 로드
- 기본값은 `useSettings.js`에 하드코딩

### 6.3 MaaS 서버와의 연결 구조

#### REST API 연결
- **프로토콜**: HTTP/HTTPS
- **인증**: OAuth 1.0
- **엔드포인트**: `{maasUrl}/MAAS/api/2.0/`
- **타임아웃**: 30초 (일반 API), 10초 (연결 테스트)

#### WebSocket 연결
- **프로토콜**: WebSocket (ws:// 또는 wss://)
- **엔드포인트**: `{maasUrl}/MAAS/ws`
- **인증**: 
  1. HTTP POST로 `/MAAS/accounts/login/`에 로그인
  2. 쿠키에서 `csrftoken` 및 `sessionid` 추출
  3. WebSocket 핸드셰이크 시 Cookie 헤더에 포함
- **재연결**: 자동 재연결 (5초 간격)
- **Heartbeat**: 연결 상태 모니터링 (60초 간격, 실제 ping은 비활성화)

### 6.4 테스트/개발 환경 분리
- 현재는 개발 환경만 지원
- 프로덕션 환경 분리를 위해서는:
  - Spring Profile 사용 (`application-dev.properties`, `application-prod.properties`)
  - 환경 변수 사용
  - CORS 설정 제한 (프로덕션에서는 특정 도메인만 허용)

### 6.5 성능 최적화 전략

#### 프론트엔드
- **병렬 API 호출**: 모든 머신의 상세 정보를 `Promise.all()`로 병렬 조회
- **페이지네이션**: 대량 머신 처리 시 페이지 단위로 로드
- **검색/필터링**: 클라이언트 사이드에서 즉시 필터링 (서버 부하 감소)
- **WebSocket 실시간 업데이트**: 폴링 대신 WebSocket으로 효율적인 상태 동기화

#### 백엔드
- **리액티브 프로그래밍**: WebFlux를 통한 비동기 처리
- **타임아웃 설정**: API 호출 타임아웃 (30초)으로 무한 대기 방지
- **에러 처리**: 개별 머신 정보 조회 실패해도 전체 프로세스 중단 방지

### 6.6 보안 고려사항

#### 현재 구현
- **OAuth 1.0 인증**: MAAS API 인증
- **CORS 설정**: 개발 환경에서는 모든 origin 허용 (프로덕션에서는 제한 필요)
- **API 키 저장**: 평문으로 JSON 파일에 저장 (보안 강화 필요)

#### 보안 개선 필요 사항
- **API 키 암호화**: 민감한 정보 암호화 저장
- **HTTPS 강제**: 프로덕션 환경에서 HTTPS 사용
- **세션 관리**: WebSocket 세션 타임아웃 및 무효화
- **입력 검증**: IP 주소, MAC 주소 등 사용자 입력 검증
- **에러 메시지**: 민감한 정보 노출 방지

---

## 7. 남은 과제 및 TODO

### 7.1 코드 내 TODO 주석

#### MachinesTab.vue
- **Line 882**: `// TODO: Implement machine details view`
  - 머신 상세 정보 뷰 구현 필요
- **Line 887**: `// TODO: Implement machine actions menu`
  - 머신 액션 메뉴 구현 필요

### 7.2 현재 미완성된 기능

#### 1. 머신 상세 정보 뷰
- **현재 상태**: 미구현
- **필요 작업**: 
  - 머신 클릭 시 상세 정보 모달 또는 페이지 표시
  - Block devices, 인터페이스 상세 정보 표시
  - 이벤트 로그 표시

#### 2. 머신 액션 메뉴
- **현재 상태**: 미구현
- **필요 작업**:
  - 드롭다운 메뉴로 추가 액션 제공
  - Tag 관리, Owner 변경, Zone/Pool 변경 등

#### 3. 배포 중단 (Abort Deploy) 기능
- **현재 상태**: UI에 버튼은 있으나 백엔드 API 미구현
- **필요 작업**:
  - `MaasController`에 `abortDeploy` 엔드포인트 추가
  - `MaasApiService`에 `abortDeploy` 메서드 추가
  - MAAS API `/MAAS/api/2.0/machines/{systemId}/op-abort` 호출 (배포 중단용)

#### 4. 다중 머신 선택 액션
- **현재 상태**: 체크박스는 있으나 다중 선택 액션 미구현
- **필요 작업**:
  - 다중 머신 선택 시 일괄 액션 버튼 표시
  - 일괄 커미셔닝, 일괄 배포 등

#### 5. 에러 처리 개선
- **현재 상태**: 기본적인 에러 처리만 구현
- **필요 작업**:
  - 사용자 친화적인 에러 메시지
  - 재시도 로직
  - 에러 로깅 및 모니터링

### 7.3 버그 및 개선 필요 사항

#### 1. WebSocket 재연결 로직
- **문제**: 특정 상황에서 무한 재연결 시도 가능
- **개선**: 재연결 횟수 제한 및 백오프 전략 추가

#### 2. 설정 파일 경로
- **문제**: `maas-ui-settings.json`이 프로젝트 루트에 저장됨
- **개선**: 사용자 홈 디렉터리 또는 설정 디렉터리에 저장

#### 3. CORS 설정
- **문제**: 모든 origin 허용 (`*`)
- **개선**: 프로덕션에서는 특정 도메인만 허용

#### 4. API 키 보안
- **문제**: API 키가 평문으로 저장됨
- **개선**: 암호화 저장 또는 환경 변수 사용

#### 5. 로깅
- **문제**: System.out.println 사용
- **개선**: SLF4J/Logback 사용으로 통일

#### 6. 타임아웃 설정
- **문제**: 하드코딩된 타임아웃 값
- **개선**: 설정 파일에서 관리

### 7.4 앞으로 개발이 필요한 기능 제안

#### 1. 사용자 인증 및 권한 관리
- 로그인/로그아웃 기능
- 역할 기반 접근 제어 (RBAC)
- 세션 관리

#### 2. 이벤트 로그 및 알림
- 머신 상태 변경 이벤트 로그
- 알림 시스템 (이메일, 웹훅 등)
- 이벤트 필터링 및 검색

#### 3. 대시보드 확장
- 차트 및 그래프 (머신 상태 추이)
- 리소스 사용률 모니터링
- 예약된 작업 목록

#### 4. 템플릿 및 프로파일 관리
- 배포 템플릿 관리
- 커스텀 스크립트 관리
- 프로파일 저장 및 재사용

#### 5. API 문서화
- Swagger/OpenAPI 통합
- API 사용 예제
- 에러 코드 문서화

#### 6. 테스트
- 단위 테스트 (JUnit, Jest)
- 통합 테스트
- E2E 테스트 (Cypress, Playwright)

#### 7. 다국어 지원
- i18n (국제화) 지원
- 언어 선택 기능

#### 8. 다크 모드
- 테마 전환 기능
- 사용자 설정 저장

---

## 8. 부록

### 8.1 MAAS API 상태 코드
- **0**: New
- **1**: Commissioning
- **2**: Failed Commissioning
- **3**: Missing
- **4**: Ready (Commissioned)
- **5**: Reserved
- **6**: Deployed
- **7**: Retired
- **8**: Broken
- **9**: Deploying
- **10**: Allocated
- **11**: Failed Deployment
- **12**: Releasing
- **13**: Failed Releasing
- **14**: Disk Erasing
- **15**: Failed Disk Erasing

### 8.2 WebSocket 에러 코드
- **1000**: 정상 종료 (재연결하지 않음)
- **1001**: Idle Timeout (재연결 시도)
- **1002**: Protocol Error (재연결 시도)
- **1003**: Unsupported Data (재연결하지 않음)
- **1007**: Invalid frame payload data (재연결하지 않음)

### 8.3 네트워크 계층 구조

#### Fabric-VLAN-Subnet 관계
```
Fabric (물리적 네트워크)
  └── VLAN (가상 LAN, Fabric 내 논리적 분리)
      └── Subnet (IP 주소 범위, VLAN에 속함)
          └── IP Address (인터페이스에 할당)
```

- **Fabric**: 물리적 네트워크 인프라를 나타냄
- **VLAN**: Fabric 내에서 논리적으로 분리된 네트워크 세그먼트
- **Subnet**: IP 주소 범위 (CIDR 표기)
- **인터페이스**: 머신의 네트워크 인터페이스
  - 하나의 인터페이스는 하나의 Fabric/VLAN에 속함
  - 하나의 인터페이스에 여러 IP 주소 할당 가능 (Primary + Secondary)

#### IP Assignment 모드
- **Unconfigured**: IP 주소 미설정
- **Automatic**: DHCP를 통한 자동 할당 (AUTO mode)
- **Static**: 수동 IP 주소 입력 (STATIC mode)

### 8.4 Block Device 및 스토리지 계산

#### Block Device 구조
- **blockdevice_set**: 머신의 모든 블록 디바이스(디스크) 목록
- **각 디바이스 속성**:
  - `id`: 디바이스 ID
  - `name`: 디바이스 이름 (예: sda, sdb)
  - `size`: 디바이스 크기 (바이트 단위)
  - `type`: 디바이스 타입 (physical, virtual 등)
  - `block_size`: 블록 크기

#### 스토리지 계산
- **디스크 수**: `blockdevice_set.length`
- **총 스토리지**: 모든 block device의 `size` 값 합산
- **표시 형식**: GB 또는 MiB 단위로 변환하여 표시

### 8.5 주요 파일 경로 요약

#### 백엔드
- 설정 파일: `backend/maas-ui-backend/maas-ui-settings.json`
- 애플리케이션 설정: `backend/maas-ui-backend/src/main/resources/application.properties`
- 메인 애플리케이션: `backend/maas-ui-backend/src/main/java/com/maas/ui/MaasUiBackendApplication.java`

#### 프론트엔드
- 메인 애플리케이션: `frontend/src/App.vue`
- 대시보드: `frontend/src/components/DashboardTab.vue`
- 머신 관리: `frontend/src/components/MachinesTab.vue`
- 설정: `frontend/src/components/SettingsTab.vue`
- 설정 Composable: `frontend/src/composables/useSettings.js`
- WebSocket Composable: `frontend/src/composables/useWebSocket.js`

---

## 문서 버전 정보
- **작성일**: 2024년
- **버전**: 1.0
- **작성자**: 시스템 리서처 및 PRD 테크 리포터

