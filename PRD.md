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
  - 전체 머신 수 (데이터를 불러오지 못한 경우 "-" 표시)
  - 커미셔닝된 머신 수 (데이터를 불러오지 못한 경우 "-" 표시)
  - 배포된 머신 수 (데이터를 불러오지 못한 경우 "-" 표시)
  - Deploy 가능한 OS 수 (데이터를 불러오지 못한 경우 "-" 표시)
- **주요 코드 파일**: 
  - `DashboardTab.vue`: UI 컴포넌트
  - `MaasController.getDashboardStats()`: API 엔드포인트
  - `MaasApiService.getAllMachines()`: MAAS API 호출
  - `MaasApiService.calculateMachineStats()`: 통계 계산
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 `loadMachineStats()` 및 `loadDeployableOSCount()` 호출
  2. 백엔드 `/api/dashboard/stats` 엔드포인트 호출
  3. 백엔드가 MAAS API로부터 모든 머신 정보 조회
  4. 머신 상태별로 카운트 계산 (status 값 기반)
  5. 통계 데이터를 프론트엔드로 반환
  6. 백엔드 `/api/deployable-os` 엔드포인트 호출하여 Deploy 가능한 OS 목록 조회
  7. OS 목록 개수를 카운트하여 Deployable OS 수 계산
  8. 카드 형태로 표시 (머신 통계는 첫 번째 줄, Deployable OS는 두 번째 줄)
- **에러 처리**: 
  - API 호출 실패 시 에러 메시지 표시
  - 데이터를 불러오지 못한 경우 통계 값에 "-" 표시
  - 네트워크 통신 실패 또는 API 호출 실패 시 모든 통계 항목에 "-" 표시

### 3.2 Configuration Tab 기능

#### 기능명: Deployable OS 이미지 목록 조회
- **설명**: MAAS에서 설치 가능한 OS 이미지 목록을 조회하여 표시하고, 각 OS별로 Deploy된 머신 수를 표시
- **입력**: 없음 (자동 로드)
- **출력**: OS 이미지 목록 테이블
  - OS, Release, Version, Architecture, Deployed Machines 컬럼
  - 같은 OS의 여러 release는 OS 이름을 rowspan으로 병합하여 표시
- **주요 코드 파일**:
  - `ConfigurationTab.vue`: Images 섹션
  - `MaasController.getDeployableOS()`: API 엔드포인트
  - `MaasApiService.getAllDeployableOS()`: MAAS API 호출
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 자동 로드
  2. 백엔드 `/api/deployable-os` 엔드포인트 호출
  3. MAAS API `/MAAS/api/2.0/boot-sources/` 호출하여 boot sources 목록 조회
  4. 각 boot source의 `/MAAS/api/2.0/boot-sources/{id}/selections/` 호출하여 selections 조회
  5. 모든 selections를 병합하여 deployable OS 목록 생성
  6. Ubuntu release 버전 매핑 (xenial → 16.04 LTS 등)
  7. OS별로 그룹화하여 테이블 형태로 표시
  8. 같은 OS의 여러 release는 OS 이름을 rowspan으로 병합
  9. 머신 목록을 로드하여 각 OS/release 조합에 대해 Deployed 상태인 머신 수 계산
  10. Deployed Machines 컬럼에 각 OS/release로 배포된 머신 수 표시
- **UI 특징**:
  - OS 이름은 첫 번째 release 행에만 표시하고 rowspan으로 병합
  - OS 이름 셀은 배경색과 테두리로 구분
  - Deployed Machines 컬럼에는 해당 OS/release로 배포된 머신 수 표시
- **에러 처리**:
  - API 호출 실패 시 에러 메시지 표시
  - Retry 버튼 제공

#### 기능명: MAAS 태그 관리
- **설명**: MAAS에 등록된 모든 태그 목록을 조회하고, 태그를 생성, 수정, 삭제할 수 있는 기능
- **입력**: 
  - 태그 생성/수정: Name (필수), Comment, Kernel Options, Definition
  - 태그 삭제: 태그 행 클릭 후 Delete 버튼
- **출력**: 태그 목록 테이블
  - Name, Definition, Comment, Kernel Options 컬럼
  - 태그 행 클릭 시 태그 정보 모달 표시
- **주요 코드 파일**:
  - `ConfigurationTab.vue`: Tags 섹션
  - `MaasController.getTags()`, `createTag()`, `updateTag()`, `deleteTag()`: API 엔드포인트
  - `MaasApiService.getAllTags()`, `createTag()`, `updateTag()`, `deleteTag()`: MAAS API 호출
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 자동 로드
  2. 백엔드 `/api/tags` 엔드포인트 호출
  3. MAAS API `/MAAS/api/2.0/tags/` 호출
  4. 태그 정보 파싱 (name, definition, comment, kernel_opts)
  5. 테이블 형태로 표시
  6. 태그 이름을 배지 스타일로 표시
  7. "+ Add Tag" 버튼 클릭 시 태그 생성 모달 표시
  8. 태그 행 클릭 시 태그 정보 모달 표시 (수정/삭제 가능)
  9. Update 버튼은 실제로 내용이 변경되었을 때만 활성화
  10. 태그 삭제 시 커스텀 확인 모달 표시
  11. 삭제 확인 후 MAAS API `DELETE /MAAS/api/2.0/tags/{name}/` 호출
  12. 204 NO_CONTENT 응답 처리
  13. 태그 목록 자동 새로고침
- **UI/UX 특징**:
  - Update 버튼은 원본 데이터와 비교하여 변경사항이 있을 때만 활성화
  - 새로 생성하는 경우는 유효성 검사만 수행
- **에러 처리**:
  - API 호출 실패 시 커스텀 알림 모달로 에러 메시지 표시
  - Retry 버튼 제공

#### 기능명: Cloud-Config 템플릿 관리
- **설명**: OS 배포 시 사용할 Cloud-Config 템플릿을 생성, 수정, 삭제
- **입력**: 
  - 템플릿 이름 (필수)
  - 태그 선택 (체크박스, 다중 선택 가능)
  - 설명 (선택)
  - Cloud-Config YAML (필수)
- **출력**: 템플릿 목록 테이블 및 편집 모달
  - Name, Tags, Description 컬럼
  - 템플릿 행 클릭 시 템플릿 정보 모달 표시
- **주요 코드 파일**:
  - `ConfigurationTab.vue`: Cloud-Config Templates 섹션
  - `MaasController.getCloudConfigTemplates()`, `saveCloudConfigTemplates()`: API 엔드포인트
  - `CloudConfigTemplateService`: 파일 기반 저장 서비스
  - `maas-cloud-config-templates.json`: 템플릿 저장 파일
- **내부 로직 흐름**:
  1. 컴포넌트 마운트 시 백엔드 API에서 템플릿 목록 로드
  2. 백엔드 `/api/cloud-config-templates` GET 요청
  3. 백엔드에서 `maas-cloud-config-templates.json` 파일 읽기
  4. 기존 데이터 마이그레이션 (단일 tag → tags 배열, base64 인코딩 값 자동 생성)
  5. 템플릿 목록 테이블 표시
  6. "+ Add Template" 버튼 클릭 시 템플릿 생성 모달 표시
  7. 템플릿 행 클릭 시 템플릿 정보 모달 표시 (수정/삭제 가능)
  8. 모달에서:
     - Name 입력
     - MAAS Tags 목록에서 체크박스로 태그 선택 (다중 선택 가능)
     - Description 입력
     - Cloud-Config YAML 입력 (코드 에디터 스타일)
  9. "Create" 또는 "Update" 버튼 클릭
  10. Update 버튼은 실제로 내용이 변경되었을 때만 활성화
  11. Cloud-Config YAML을 base64 인코딩하여 `cloudConfigBase64` 필드 생성
  12. 백엔드 `/api/cloud-config-templates` POST 요청으로 템플릿 데이터 저장
  13. 백엔드에서 `maas-cloud-config-templates.json` 파일에 저장
  14. 템플릿 목록 새로고침
  15. 템플릿 삭제 시 커스텀 확인 모달 표시
  16. 삭제 확인 후 템플릿 목록에서 제거 및 백엔드에 저장
  17. 삭제된 템플릿이 편집 중이었다면 모달 자동 닫기
- **태그 연계**:
  - 하나의 템플릿은 여러 태그와 연관 가능 (tags 배열)
  - Deploy 시 머신의 태그와 템플릿의 태그를 비교하여 매칭
  - 매칭되는 템플릿이 "Recommended" 그룹에 우선 표시
- **데이터 저장**:
  - 백엔드 서버의 `maas-cloud-config-templates.json` 파일에 JSON 형식으로 저장
  - 프로젝트 루트 디렉토리에 저장 (서버 재시작 후에도 유지)
  - 각 템플릿은 다음 필드 포함:
    - `id`: 고유 식별자
    - `name`: 템플릿 이름
    - `tags`: 연관된 태그 배열
    - `description`: 설명
    - `cloudConfig`: Cloud-Config YAML 원본
    - `cloudConfigBase64`: base64 인코딩된 Cloud-Config YAML (Deploy 시 사용)
    - `createdAt`: 생성 일시
    - `updatedAt`: 수정 일시
  - 기존 템플릿 로드 시 `cloudConfigBase64`가 없으면 자동 생성
- **UI/UX 특징**:
  - 템플릿 행 클릭 시 모달 표시 (Tags와 동일한 방식)
  - 모달 제목: "Template Information" (편집 시)
  - Update 버튼은 원본 데이터와 비교하여 변경사항이 있을 때만 활성화
  - Delete 버튼은 모달 footer에 표시 (편집 모드일 때만)
  - 기존 템플릿 수정 시 `createdAt` 필드 유지
- **에러 처리**:
  - 필수 필드 검증 (Name, Cloud-Config YAML)
  - API 호출 실패 시 커스텀 알림 모달로 에러 메시지 표시
  - 파일 저장 실패 시 에러 메시지 반환

### 3.3 Machines Tab 기능

#### 기능명: 머신 목록 조회 및 표시
- **설명**: MAAS에 등록된 모든 머신을 테이블 형태로 표시
- **입력**: 
  - 검색 쿼리 (선택)
  - 상태 필터 (All, New, Commissioning, Ready, Allocated, Deployed, Failed)
  - 페이지 번호
- **출력**: 머신 목록 테이블
  - FQDN, Power State/Power Type, Status, Owner/Tags, Pool, Zone, Fabric
  - Status 컬럼: 진행 중인 상태일 때 회전하는 로딩 스피너 아이콘 표시
  - CPU Cores, RAM, Disk Count, Storage
  - 액션 버튼 (Commission, Network, Deploy)
  - Power Action (Turn on, Turn off) - 드롭다운 메뉴
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

#### 기능명: IP 주소 및 MAC 주소 처리
- **설명**: 머신의 IP 주소와 MAC 주소를 안전하게 추출, 정규화, 표시하는 기능입니다.
- **주요 기능**:
  - **IP 주소 추출 (`extractIpAddresses`)**: `interface_set`과 `boot_interface`의 `links` 배열에서 IP 주소를 추출
  - **IP 주소 정규화 (`normalizeIpAddresses`)**: 다양한 형식(배열, JSON 문자열, 객체)의 IP 주소 데이터를 배열로 정규화
  - **MAC 주소 정규화 (`normalizeMacAddresses`)**: 다양한 형식의 MAC 주소 데이터를 배열로 정규화
  - **안전한 표시 함수 (`getFirstIpAddress`, `getFirstMacAddress`)**: 첫 번째 IP/MAC 주소를 안전하게 가져와서 표시
- **검색 기능**:
  - 검색 시 IP 주소와 MAC 주소를 정규화하여 검색 정확도 향상
  - 다양한 데이터 형식에서도 검색 가능
- **주요 코드 파일**:
  - `MachinesTab.vue`:
    - `extractIpAddresses()`: interface_set과 boot_interface에서 IP 주소 추출
    - `normalizeIpAddresses()`: IP 주소 데이터 정규화
    - `normalizeMacAddresses()`: MAC 주소 데이터 정규화
    - `getFirstIpAddress()`: 첫 번째 IP 주소 안전하게 반환
    - `getFirstMacAddress()`: 첫 번째 MAC 주소 안전하게 반환

#### 기능명: 커스텀 모달 (Custom Modal)
- **설명**: 브라우저 기본 `window.confirm()`과 `window.alert()`를 대체하는 커스텀 모달 컴포넌트입니다.
- **주요 기능**:
  - **Confirmation Modal**: 사용자 확인을 받는 모달 (확인/취소 버튼)
  - **Alert Modal**: 정보를 표시하는 모달 (확인 버튼만)
- **사용 사례**:
  - 재커미셔닝 확인 메시지
  - Release 확인 메시지
  - 일괄 작업 확인 메시지
  - 삭제 확인 메시지 (머신, 태그, 템플릿)
  - 에러 알림 메시지
- **주요 코드 파일**:
  - `MachinesTab.vue`:
    - `customConfirm()`: Promise 기반 확인 모달 표시
    - `customAlert()`: Promise 기반 알림 모달 표시
    - `showConfirmModal`, `confirmModalTitle`, `confirmModalMessage`, `confirmModalButtonText`
    - `showAlertModal`, `alertModalTitle`, `alertModalMessage`
    - `confirmAction()`, `cancelConfirm()`, `closeAlert()`
  - `ConfigurationTab.vue`:
    - 동일한 `customConfirm()`, `customAlert()` 함수 사용
    - 태그 삭제, 템플릿 삭제 시 확인 모달 사용
    - 에러 메시지 표시 시 알림 모달 사용
- **UI/UX 특징**:
  - 모든 모달은 드래그 가능 (모달 헤더를 드래그하여 이동)
  - 모달 바깥쪽 클릭 시 닫히지 않음 (확인/취소 버튼으로만 닫힘)
  - Teleport를 사용하여 body에 렌더링
  - 일관된 디자인 스타일
  - 모달 헤더에 grab/grabbing 커서 표시

#### 기능명: 상태별 머신 선택 (Status-based Machine Selection)
- **설명**: 머신 테이블의 전체 선택 체크박스 옆에 드롭다운 메뉴를 제공하여, 머신 상태별로 일괄 선택/해제 기능을 제공합니다. 진행 중인 상태(Commissioning, Deploying, Disk Erasing 등)는 제외하고, 멈춰 있는 상태(New, Ready, Allocated, Deployed, Failed 등)만 선택 가능합니다.
- **입력**: 
  - 전체 선택 체크박스 옆 드롭다운 아이콘(▼) 클릭
  - 드롭다운 메뉴에서 상태별 체크박스 선택/해제
- **출력**: 
  - 선택된 상태에 해당하는 모든 머신이 자동으로 선택됨
  - 드롭다운 메뉴의 체크박스 상태 업데이트
  - 머신 테이블의 개별 체크박스 상태 업데이트
- **주요 코드 파일**:
  - `MachinesTab.vue`: 
    - `availableStatusesForSelection`: 멈춰 있는 상태 목록 계산 (computed)
    - `selectedStatusesForSelection`: 선택된 상태 목록 (ref)
    - `toggleSelectByStatus()`: 상태별 선택 토글 함수
    - `toggleStatusSelectMenu()`: 드롭다운 메뉴 열기/닫기
    - `isStatusSelected()`: 특정 상태 선택 여부 확인
    - `getStatusDisplayName()`: 상태 표시 이름 변환
    - `isStatusInProgress()`: 진행 중 상태 판별 함수
- **내부 로직 흐름**:
  1. 전체 선택 체크박스 옆 드롭다운 아이콘(▼)이 항상 표시됨
  2. 드롭다운 아이콘 클릭 시 상태 선택 메뉴 표시
  3. `availableStatusesForSelection` computed 속성이 현재 필터링된 머신 목록에서 진행 중 상태를 제외한 모든 상태를 추출
  4. 드롭다운 메뉴에 각 상태가 체크박스와 함께 표시됨
  5. 사용자가 상태 체크박스 또는 텍스트 클릭 시:
     - `toggleSelectByStatus()` 함수 호출
     - 선택된 상태 목록(`selectedStatusesForSelection`) 업데이트
     - 해당 상태를 가진 모든 머신의 ID를 `selectedMachines`에 추가/제거
     - `updateSelectAllState()` 호출하여 전체 선택 체크박스 상태 업데이트
  6. 전체 선택 체크박스 클릭 시:
     - 모든 머신 선택/해제
     - 모든 멈춰 있는 상태를 선택된 상태 목록에 추가/제거
- **상태 필터링 규칙**:
  - **포함되는 상태**: New, Ready, Allocated, Deployed, Failed, Reserved, Retired, Broken 등 (진행 중이 아닌 모든 상태)
  - **제외되는 상태**: Commissioning, Deploying, Disk Erasing, Releasing 등 (진행 중인 상태, "ing" 또는 "erasing"으로 끝나는 상태)
- **UI/UX 특징**:
  - 드롭다운 아이콘은 항상 표시되어 레이아웃 흔들림 방지
  - 드롭다운 메뉴는 Power 드롭다운과 동일한 스타일 적용
  - 체크박스 클릭 시에도 동작 (mousedown 이벤트 사용)
  - 상태 텍스트 클릭 시에도 동작
  - 선택된 상태는 체크박스에 체크 표시
  - 외부 클릭 시 드롭다운 메뉴 자동 닫힘
- **에러 처리**:
  - 상태 목록이 비어있는 경우 빈 드롭다운 표시
  - 머신 목록이 변경되어도 선택된 상태 목록 유지

#### 기능명: 일괄 머신 액션 (Bulk Machine Actions)
- **설명**: 선택된 머신들에 대해 일괄로 액션을 수행할 수 있는 액션 바를 제공합니다. 머신을 하나 이상 선택하면 화면 상단 헤더 영역에 액션 바가 나타나며, 여러 머신에 대해 동시에 작업을 수행할 수 있습니다.
- **입력**: 
  - 머신 체크박스 선택 (하나 이상)
  - 액션 바의 Actions 드롭다운 메뉴에서 액션 선택
  - 액션 바의 Power 드롭다운 메뉴에서 전원 액션 선택 (예정)
  - 액션 바의 Delete 버튼 클릭
- **출력**: 
  - 선택된 머신 수 표시 ("X selected")
  - 액션 수행 결과 (성공/실패 메시지)
  - 머신 목록 자동 새로고침
- **주요 코드 파일**:
  - `MachinesTab.vue`:
    - `action-bar`: 액션 바 컴포넌트 (v-if="selectedMachines.length > 0")
    - `toggleActionsMenu()`: Actions 드롭다운 메뉴 열기/닫기
    - `togglePowerActionMenu()`: Power 드롭다운 메뉴 열기/닫기
    - `handleBulkAction()`: 일괄 액션 핸들러 (Commission, Allocate, Deploy, Release, Abort)
    - `handleBulkDelete()`: 일괄 삭제 핸들러
    - `canBulkCommission()`, `canBulkDeploy()`, `canBulkRelease()`, `canBulkAbort()`: 액션 가능 여부 확인 함수
    - `getSelectedMachines()`: 선택된 머신 객체 목록 반환
  - `MaasController.deleteMachine()`: DELETE 엔드포인트
  - `MaasApiService.deleteMachine()`: MAAS API DELETE 호출
- **내부 로직 흐름**:
  1. 사용자가 머신 체크박스를 선택하면 `selectedMachines` 배열에 머신 ID 추가
  2. `selectedMachines.length > 0`이 되면 액션 바가 화면 상단에 표시됨
  3. 액션 바 구성:
     - **Actions 드롭다운**: Commission, Allocate, Deploy, Release, Abort
     - **Power 드롭다운**: Turn on, Turn off (Coming soon - 비활성화)
     - **Delete 버튼**: 직접 클릭 가능
     - **선택된 머신 수 표시**: "X selected"
  4. Actions 드롭다운에서 액션 선택 시:
     - `handleBulkAction(action)` 호출
     - 확인 메시지 표시
     - 선택된 각 머신에 대해 해당 액션 수행 (상태 확인 후)
     - 각 머신의 상태에 따라 액션 가능 여부 판단
  5. Delete 버튼 클릭 시:
     - `handleBulkDelete()` 호출
     - 확인 메시지 표시 ("이 작업은 되돌릴 수 없습니다")
     - 선택된 모든 머신에 대해 병렬로 DELETE API 호출
     - 성공/실패 결과 집계 및 메시지 표시
     - 500ms 대기 후 머신 목록 새로고침
     - 선택 해제
- **액션별 동작 규칙**:
  - **Commission**: 선택된 머신 중 Commission 가능한 머신만 처리 (`canCommission()` 체크)
  - **Allocate**: 현재 API 미구현으로 항상 비활성화
  - **Deploy**: Ready 또는 Allocated 상태인 머신만 처리
  - **Release**: Failed Deployment, Failed Disk Erasing, Deployed, 또는 Allocated 상태인 머신만 처리
  - **Abort**: Commissioning 또는 Deploying 상태인 머신만 처리
  - **Delete**: 모든 선택된 머신에 대해 삭제 수행 (상태 무관)
- **백엔드 API**:
  - **DELETE `/api/machines/{systemId}`**: 머신 삭제
    - 요청 파라미터: `maasUrl`, `apiKey`
    - MAAS API: `DELETE /MAAS/api/2.0/machines/{systemId}/`
    - 응답: 성공 시 200 OK, 실패 시 에러 메시지
- **UI/UX 특징**:
  - 액션 바는 헤더 영역 오른쪽에 표시 (연결 상태 표시기 왼쪽)
  - 흰색 배경, 그림자 효과, 둥근 모서리
  - 드롭다운 메뉴는 Power 드롭다운과 동일한 스타일
  - 비활성화된 액션은 회색으로 표시되고 클릭 불가
  - 선택된 머신 수는 액션 바 오른쪽에 표시
  - 외부 클릭 시 드롭다운 메뉴 자동 닫힘
  - **고정 크기 및 레이아웃 안정성**:
    - 액션 바의 크기는 선택된 머신 수에 관계없이 고정되어 레이아웃 흔들림을 방지합니다.
    - 선택된 머신 수 표시 영역(`action-bar-selected-count`)은 최소 너비 120px로 고정되어 100개 이상의 머신이 선택되어도 크기가 변하지 않습니다.
    - Actions와 Power 드롭다운 아이콘(v/^)의 너비가 고정되어 아이콘 변경 시에도 액션 바 크기가 변하지 않습니다.
    - 머신 row의 높이는 MAC/IP 정보가 없어도 일정하게 유지됩니다 (`min-height: 2.2rem`).
    - MAC/IP 정보가 없는 경우에도 빈 공간을 유지하여 row 높이를 일정하게 유지합니다.
  - **드롭다운 메뉴 동작**:
    - Actions와 Power 드롭다운 메뉴는 버튼을 다시 클릭하거나 화면의 다른 부분을 클릭하면 자동으로 닫힙니다.
    - 외부 클릭 감지 로직을 통해 사용자 경험을 개선했습니다.
- **에러 처리**:
  - 일괄 삭제 시 일부 실패해도 성공한 머신은 삭제됨
  - 실패한 머신의 ID와 에러 메시지를 알림으로 표시
  - HTTP 2xx 상태 코드는 성공으로 간주 (DELETE는 응답 본문이 없을 수 있음)
  - 각 액션은 개별 머신에 대해 try-catch로 에러 처리
  - 액션 수행 후 머신 목록 자동 새로고침

#### 기능명: 머신 추가 (Add Machine)
- **설명**: 새로운 물리적 서버를 MAAS에 등록
- **입력**:
  - Hostname (선택)
  - Architecture (기본값: amd64)
  - MAC Addresses (필수, 쉼표로 구분)
  - Power Type (기본값: manual, 옵션: manual, ipmi)
  - Commission 여부 (기본값: false)
    - **IPMI 선택 시**: 자동으로 true로 설정됨
  - Description (선택)
  - **IPMI Power Type 전용 필드** (Power Type이 "ipmi"일 때만 표시):
    - Power Driver (기본값: LAN_2_0, 옵션: LAN [IPMI 1.5], LAN_2_0 [IPMI 2.0])
    - Power Boot Type (기본값: auto, 옵션: Automatic, Legacy boot, EFI boot)
    - IP Address (선택)
    - Power User (선택)
    - Power Password (선택)
    - K_g BMC key (선택)
    - Cipher Suite ID (기본값: 3, 옵션: 17, 3, '', 8, 12)
    - Privilege Level (기본값: OPERATOR, 옵션: USER, OPERATOR, ADMIN)
    - Workaround Flags (기본값: ['opensesspriv'], 다중 선택 가능)
    - Power MAC (선택)
- **출력**: 추가된 머신 정보
- **주요 코드 파일**:
  - `MachinesTab.vue`: 모달 UI 및 폼 처리
  - `MaasController.addMachine()`: API 엔드포인트
  - `MaasApiService.addMachine()`: MAAS API 호출
- **내부 로직 흐름**:
  1. "Add Machine" 버튼 클릭 → 모달 표시
  2. 사용자가 폼 입력
  3. Power Type이 "ipmi"로 변경되면:
     - Commission 자동으로 "true"로 설정
     - IPMI Configuration 섹션 표시 (박스로 그룹핑)
  4. MAC 주소 형식 검증
  5. 백엔드 `/api/machines` POST 요청
     - **Form Data 형식 사용** (`application/x-www-form-urlencoded`)
     - 빈 값 필드는 form data에 포함하지 않음
  6. MAAS API `/MAAS/api/2.0/machines/` POST 요청
     - **Form Data 형식 사용** (`BodyInserters.fromFormData()`)
     - IPMI 파라미터는 `power_parameters_` 접두사로 전송
  7. 성공 시 머신 목록 새로고침
- **UI/UX 특징**:
  - IPMI Configuration 필드들은 박스로 그룹핑되어 시각적으로 구분됨
  - IPMI Configuration 헤더로 섹션 구분
  - 입력되지 않은 필드는 form data에 포함되지 않음
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
- **에러 처리**: 
  - API 오류 메시지 표시
  - 503 Service Unavailable 또는 타임아웃 에러 발생 시:
    - "Abort operation may have timed out, but the operation might still be in progress. Refreshing machine status..." 메시지 표시
    - 2초 후 자동으로 머신 상태 재확인 (`loadMachines()` 호출)
- **특수 동작**:
  - Failed Deployment 상태에서 Abort 시도 시: 자동으로 Release 작업으로 전환
  - Deploying 상태에서 Abort 시도 시: 정상적으로 Abort 처리

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
  - **Fabric Disconnect 기능**: "Disconnect" 옵션 선택 시 인터페이스의 VLAN 연결을 제거 (vlan=""로 설정)
  - **조건부 필드 표시**: IP Assignment Mode 및 관련 필드(IP 주소, Secondary IP 등)는 Fabric이 선택된 경우에만 표시
  - **Secondary IP 관리**: 여러 개의 Secondary IP 주소를 동적으로 추가/제거 가능
  - **IP 주소 자동 완성**: Subnet CIDR 기반으로 네트워크 프리픽스 자동 제안
- **Fabric 변경 및 IP 설정 통합 처리**:
  - **원래 Fabric ID 저장**: 네트워크 인터페이스 로드 시 `originalFabricId`를 저장하여 Fabric 변경 전 원래 값을 보관합니다.
  - **Fabric 변경 시 처리 순서**:
    1. Fabric 변경 감지 시 먼저 원래 fabric ID로 기존 link를 unlink 시도
    2. unlink 실패해도 계속 진행 (Fabric 변경은 이미 성공했을 수 있고, link가 이미 제거되었을 수 있음)
    3. 새로운 fabric로 VLAN 업데이트
    4. Fabric 변경 후 최신 머신 정보를 가져와서 link ID와 subnet 업데이트
  - **IP Assignment 저장 시 처리**:
    - Static IP 저장 시 기존 link unlink 실패해도 계속 진행 (link가 이미 제거되었을 수 있음)
    - Fabric 변경 후 link가 없으면 `originalPrimaryLinkId`를 null로 설정하여 새 link 생성
  - **에러 처리**: unlink 실패 시 `console.warn`으로 경고만 출력하고 계속 진행하여, Fabric 변경과 IP Assignment 저장이 한 번에 완료되도록 구현되어 있습니다.
  - **동작**: Fabric 변경과 IP Assignment를 Static으로 설정하고 IP를 입력한 후 Save Changes를 한 번에 실행하면, unlink 실패가 있어도 Fabric 변경과 IP Assignment 저장이 모두 완료되어 2단계 작업이 1단계로 처리됩니다.

#### 기능명: 머신 배포 (Deploy)
- **설명**: 커미셔닝된 머신에 OS를 배포하며, Cloud-Config 템플릿을 선택하여 userdata를 설정할 수 있습니다
- **입력**: 
  - System ID
  - OS 선택 (OS 이름, Release, Architecture)
  - Cloud-Config 템플릿 선택 (선택사항)
  - Custom Cloud-Config 입력 (선택사항)
- **출력**: 배포 작업 시작 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: Deploy 버튼 클릭 핸들러, Deploy 모달
  - `MaasController.deployMachine()`: API 엔드포인트
  - `MaasApiService.deployMachine()`: MAAS API 호출
  - `ConfigurationTab.vue`: Cloud-Config 템플릿 관리
- **내부 로직 흐름**:
  1. "Deploy" 버튼 클릭 (Ready 또는 Allocated 상태에서만 활성화)
  2. Deploy 모달 표시
  3. Deployable OS 목록 로드 (`/api/deployable-os` 호출, 최초 1회만)
  4. Cloud-Config 템플릿 목록 로드 (백엔드 API에서)
  5. 머신의 태그와 매칭되는 템플릿 자동 필터링
  6. OS 목록 정렬 및 기본값 설정:
     - OS 이름별로 그룹화
     - 각 OS별 최신 release를 기본값으로 자동 선택
     - OS 이름 → release (최신순) 정렬
  7. 템플릿 목록 표시:
     - "Recommended (matches machine tags)" 그룹: 머신 태그와 매칭되는 템플릿 우선 표시
     - "Other Templates" 그룹: 매칭되지 않은 템플릿
     - "None" 옵션: Cloud-Config 없이 배포
     - "Custom..." 옵션: 직접 Cloud-Config YAML 입력
  8. 자동 템플릿 선택:
     - 머신의 태그와 매칭되는 템플릿이 정확히 1개인 경우 자동으로 선택
     - 매칭되는 템플릿이 0개 또는 2개 이상인 경우 "None"이 기본값
  9. 사용자가 OS 및 Cloud-Config 템플릿 선택:
     - OS는 필수 선택
     - Cloud-Config 템플릿은 선택사항 (None, 템플릿 선택, Custom)
  10. 템플릿 선택 시 미리보기 표시
  11. "Deploy" 버튼 클릭
  12. Cloud-Config YAML 처리:
     - 선택된 템플릿이 있으면 해당 템플릿의 Cloud-Config YAML 사용
     - Custom이 선택되었으면 사용자가 입력한 YAML 사용
     - YAML이 있으면 base64 인코딩하여 `userdata` 파라미터에 추가
  13. 백엔드 `/api/machines/{systemId}/deploy` POST 요청
     - 요청 파라미터: `maasUrl`, `apiKey`, `os`, `release`, `arch`, `userdata` (선택)
     - `userdata`: base64 인코딩된 Cloud-Config YAML
  14. 백엔드에서 Form Data 생성:
     - `distro_series`: `os/release` 형식 (예: `ubuntu/noble`, `ubuntu/jammy`)
     - `architecture`: 선택된 architecture (있는 경우)
     - `user_data`: base64 인코딩된 Cloud-Config YAML (선택된 템플릿 또는 Custom YAML)
  15. MAAS API `/MAAS/api/2.0/machines/{systemId}/op-deploy` POST 요청
     - **Form Data 형식 사용** (`application/x-www-form-urlencoded`)
     - `BodyInserters.fromFormData()` 사용
  16. 성공 시:
     - 모달 자동 닫기
     - 버튼이 "Abort"로 변경
     - 머신 상태가 "Deploying"으로 변경
     - 선택된 OS 정보 저장 (`deployingOS`, `deployingRelease`)
     - STATUS 컬럼에 OS 버전 표시 (예: "Ubuntu 24.04")
  17. WebSocket을 통한 상태 업데이트 수신
- **UI/UX 특징**:
  - Deploy 모달: OS 선택과 Cloud-Config 설정을 한 화면에서 처리
  - OS 선택 드롭다운: 기본값 OS 자동 선택
  - 템플릿 선택 드롭다운: 머신 태그와 매칭되는 템플릿 우선 표시
  - 템플릿 미리보기: 선택된 템플릿의 Cloud-Config YAML 표시
  - Custom Cloud-Config: YAML 에디터 (monospace 폰트)
  - 모달 드래그 가능: Network 모달과 동일한 드래그 기능
- **태그 기반 템플릿 추천**:
  - 머신의 태그와 템플릿의 태그를 비교하여 매칭
  - 하나의 템플릿은 여러 태그를 가질 수 있음 (tags 배열)
  - 하나의 머신도 여러 태그를 가질 수 있음
  - 매칭되는 템플릿이 여러 개일 경우 모두 "Recommended" 그룹에 표시
  - 사용자가 최종 템플릿 선택
- **에러 처리**:
  - 배포 불가능한 상태에서는 버튼 비활성화
  - API 오류 메시지 표시
  - Deployable OS 목록 로드 실패 시 기본값 사용
  - Cloud-Config 템플릿 로드 실패 시 "None" 옵션만 표시

#### 기능명: Power 관리 (Power Management)
- **설명**: 머신의 전원 상태 확인 및 전원 제어 (켜기/끄기)
- **입력**: 
  - System ID
  - Power Action (on/off/check)
- **출력**: 전원 상태 변경 결과 또는 전원 상태 조회 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: Power 컬럼 UI 및 드롭다운 메뉴
  - `MaasController.powerOnMachine()`, `MaasController.powerOffMachine()`, `MaasController.queryPowerState()`: API 엔드포인트
  - `MaasApiService.powerOnMachine()`, `MaasApiService.powerOffMachine()`, `MaasApiService.queryPowerState()`: MAAS API 호출
  - `togglePowerMenu()`: Power 드롭다운 메뉴 토글
  - `handlePowerAction()`: Power 액션 처리 (Turn on/Turn off)
  - `handleCheckPower()`: Power 상태 조회 처리 (Check power)
- **내부 로직 흐름**:
  1. Power 컬럼에 Power State와 Power Type 표시
  2. Power 컬럼에 마우스 호버 시 드롭다운 아이콘(▼) 표시
  3. 드롭다운 아이콘 클릭 시 Power 액션 메뉴 표시
  4. Power 상태에 따라 메뉴 항목 조건부 표시:
     - Power가 `off` 상태이면 "Turn on"만 표시
     - Power가 `on` 상태이면 "Turn off"만 표시
     - "Check power"는 항상 표시 (모든 Power 상태에서 사용 가능)
  5. **Turn on/Turn off 선택 시**:
     - `handlePowerAction()` 함수 호출
     - 백엔드 API 호출 (`POST /api/machines/{systemId}/power-on` 또는 `power-off`)
     - MAAS API 호출 (`POST /MAAS/api/2.0/machines/{systemId}/op-power_on` 또는 `op-power_off`)
     - 성공 시 2초 후 머신 목록 자동 새로고침 (WebSocket을 통한 실시간 업데이트 대기)
  6. **Check power 선택 시**:
     - `handleCheckPower()` 함수 호출 (다른 액션과 완전히 분리)
     - 백엔드 API 호출 (`GET /api/machines/{systemId}/query-power-state`)
     - MAAS API 호출 (`GET /MAAS/api/2.0/machines/{systemId}/op-query_power_state`)
     - 응답의 `state` 값(on, off, unknown 등)으로 머신의 `power_state`만 업데이트
     - 다른 작업(전원 켜기/끄기, 머신 목록 새로고침 등)은 수행하지 않음
- **에러 처리**:
  - API 호출 실패 시 에러 메시지 표시
  - Power 상태는 WebSocket을 통해 실시간으로 업데이트됨
  - Check power 실패 시 에러 메시지 표시, 전원 상태는 변경되지 않음
- **UI 구성**:
  - **Power 컬럼**: Power State (상단), Power Type (하단) 표시
    - **Power State 표시**:
      - LED 아이콘 + 텍스트 형식
      - **on**: 녹색 LED (`#28a745`) + "On" 텍스트
      - **off**: 회색 LED (`#6c757d`) + "Off" 텍스트
      - **unknown**: 노란색 LED (`#ffc107`) + "Unknown" 텍스트
      - **error/failed**: 빨간색 LED (`#dc3545`) + "Error" 텍스트 (IPMI 연결 오류 또는 전원 제어 오류 시)
    - **Power Type 표시**:
      - `manual` 타입은 `Manual`로 표시 (첫 글자 대문자)
      - `ipmi` 타입은 `IPMI`로 표시 (대문자)
      - `unknown` 타입은 `Unknown`으로 표시 (첫 글자 대문자)
      - 기타 타입도 첫 글자 대문자로 표시
  - **드롭다운 메뉴**: 호버 또는 클릭 시 표시되는 컨텍스트 메뉴
    - "TAKE ACTION:" 헤더
    - "Turn on" 옵션 (녹색 아이콘) - Power가 off 상태일 때만 표시
    - "Turn off" 옵션 (검은색 아이콘) - Power가 on 상태일 때만 표시
    - "Check power" 옵션 (🔍 아이콘) - 항상 표시
  - **웹소켓 업데이트**:
    - IPMI 머신의 power_state가 웹소켓으로 실시간 업데이트됨
    - power_state가 없을 경우 기존 값 유지
- **Check power 기능 상세**:
  - **목적**: MAAS에서 IPMI 상태 업데이트 주기가 길기 때문에 수동으로 전원 상태를 확인
  - **동작**: 전원 상태를 조회만 하고 변경하지 않음
  - **API**: GET 방식으로 호출 (`/api/machines/{systemId}/query-power-state`)
  - **응답 형식**: `{ "success": true, "state": "off" }` (state 값: on, off, unknown 등)
  - **상태 업데이트**: 응답의 `state` 값으로 머신의 `power_state`만 업데이트
  - **다른 액션과의 분리**: `handleCheckPower()` 함수는 `handlePowerAction()`과 완전히 분리되어 있어 다른 작업을 수행하지 않음

#### 기능명: 머신 릴리스 (Release)
- **설명**: 배포 실패한 머신, 배포된 머신, 또는 할당된 머신을 릴리스하여 다시 사용 가능한 상태로 변경
- **입력**: System ID
- **출력**: 릴리스 결과
- **주요 코드 파일**:
  - `MachinesTab.vue`: Release 버튼 클릭 핸들러
  - `MaasController.releaseMachine()`: API 엔드포인트
  - `MaasApiService.releaseMachine()`: MAAS API 호출
- **내부 로직 흐름**:
  1. Failed Deployment, Failed Disk Erasing, Deployed, 또는 Allocated 상태의 머신에서 "Release" 버튼 클릭
  2. 확인 메시지 표시 (개별 Release 버튼 클릭 시)
  3. 백엔드 `/api/machines/{systemId}/release` POST 요청
  4. MAAS API `/MAAS/api/2.0/machines/{systemId}/op-release` POST 요청 (force=true)
  5. 성공 시 머신 상태가 "New"로 변경
- **에러 처리**: API 오류 메시지 표시
- **사용 가능한 상태**:
  - **Failed Deployment**: 배포 실패한 머신을 릴리스
  - **Failed Disk Erasing**: 디스크 지우기 실패한 머신을 릴리스
  - **Deployed**: 배포된 머신을 릴리스
  - **Allocated**: 할당된 머신을 릴리스

#### 기능명: 머신 상세 정보 조회 (Machine Details View)
- **설명**: 머신의 상세 정보를 탭 구조로 표시하는 모달 팝업
- **입력**: 
  - FQDN 컬럼의 호스트명 클릭
  - System ID
- **출력**: 머신 상세 정보 모달 (5개 탭)
  - **Overview 탭**: 기본 정보
    - Status (상태 배지 및 상태 메시지)
    - System ID
    - Hostname
    - Owner
    - Tags (태그 목록)
    - Pool
    - Zone
    - Description (있는 경우)
    - Power Type (manual은 Manual로, ipmi는 IPMI로, unknown은 Unknown으로 표시)
    - Power State (LED 아이콘 + 텍스트)
  - **Power 탭**: 전원 설정 정보 및 편집
    - **읽기 모드**:
      - Power Type (ipmi는 IPMI로, manual은 Manual로, unknown은 Unknown으로 표시)
      - Power State (LED 아이콘 + 텍스트)
      - **IPMI 타입인 경우**: IPMI 파라미터 표시
        - Power Driver (LAN_2_0은 "LAN_2_0 [IPMI 2.0]"으로 표시)
        - Power Boot Type (auto는 "Automatic"으로 표시)
        - IP Address
        - Power User
        - Power Password (마스킹: "••••••••")
        - K_g BMC key (없으면 "-" 표시)
        - Cipher Suite ID (3은 "3 - HMAC-SHA1::HMAC-SHA1-96::AES-CBC-128"으로 표시)
        - Privilege Level (OPERATOR는 "Operator"로 표시)
        - Workaround Flags (포맷팅된 목록)
        - Power MAC (없으면 "-" 표시)
      - **Manual 타입인 경우**: Power Type과 Power State만 표시
      - Edit 버튼 (하단 고정)
    - **편집 모드**:
      - Power Type 선택 (Manual ↔ IPMI)
        - Power Type 필드는 상단에 독립적으로 배치
        - Manual 선택 시: 하부 Configuration 섹션 숨김
        - IPMI 선택 시: "IPMI Configuration" 타이틀과 함께 IPMI 설정 필드 표시
      - **IPMI Configuration** (IPMI 선택 시에만 표시):
        - Power Driver (LAN [IPMI 1.5], LAN_2_0 [IPMI 2.0])
        - Power Boot Type (Automatic, Legacy boot, EFI boot)
        - IP Address (텍스트 입력)
        - Power User (텍스트 입력)
        - Power Password (비밀번호 입력, 비워두면 기존 값 유지)
        - K_g BMC key (텍스트 입력)
        - Cipher Suite ID (드롭다운 선택)
        - Privilege Level (User, Operator, Administrator)
        - Workaround Flags (체크박스 다중 선택)
        - Power MAC (텍스트 입력)
      - Cancel 버튼 (편집 취소)
      - Save 버튼 (저장)
      - 편집 폼은 3열 그리드 레이아웃 (반응형: 2열 → 1열)
      - Workaround Flags 체크박스는 3열 그리드 (반응형: 2열 → 1열)
      - 편집 폼 내용이 많을 경우 내부 스크롤 가능 (부모 모달 스크롤 방지)
      - Edit/Cancel/Save 버튼은 항상 하단에 고정 표시
  - **Hardware 탭**: 하드웨어 정보
    - CPU Architecture
    - CPU Cores
    - Memory (포맷팅된 크기)
    - Total Storage (Block Devices에서 계산)
    - Disk Count
    - Block Devices 테이블 (Name, Type, Size, Model)
  - **Network 탭**: 네트워크 인터페이스 상세 정보
    - 각 인터페이스별 정보:
      - Interface ID
      - MAC Address
      - Fabric
      - VLAN
      - IP Addresses (Primary 및 Secondary)
      - Link Speed
  - **Operating System 탭**: OS 정보
    - Operating System
    - Distro Series
    - OS Version (Ubuntu의 경우 버전 매핑 표시, 예: "Ubuntu 24.04 LTS")
    - HWE Kernel
    - Deployment Status
  - **Events 탭**: 이벤트 히스토리
    - MAAS 서버의 이벤트 로그를 실시간으로 조회 및 표시
    - 현재 머신의 system_id와 일치하는 이벤트만 필터링하여 표시
    - 이벤트 정보: 날짜/시간, 레벨 (INFO, WARNING, ERROR, CRITICAL), 타입, 설명, 사용자
    - 날짜 순으로 정렬 (최신순)
    - 현재 상태 정보도 "CURRENT" 레벨로 함께 표시
    - 스크롤 가능한 목록 (이벤트가 많을 경우)
- **백엔드 API**:
  - **GET `/api/machines/{systemId}`**: 머신 상세 정보 조회
    - 요청 파라미터: `maasUrl`, `apiKey`
    - 응답: 머신의 전체 정보 (interface_set, osystem, distro_series 등 포함)
  - **GET `/api/machines/{systemId}/block-devices`**: Block Devices 정보 조회
    - 요청 파라미터: `maasUrl`, `apiKey`
    - 응답: Block Devices 목록 (`results` 또는 `blockdevice_set` 필드)
  - **GET `/api/machines/{systemId}/power-parameters`**: IPMI Power 파라미터 조회
    - 요청 파라미터: `maasUrl`, `apiKey`
    - 응답: IPMI Power 파라미터 정보 (power_type이 "ipmi"인 경우만)
    - MAAS API: `GET /MAAS/api/2.0/machines/{systemId}/op-power_parameters`
  - **PUT `/api/machines/{systemId}/power-parameters`**: Power 파라미터 업데이트
    - 요청 형식: **Form Data** (`application/x-www-form-urlencoded`)
    - 요청 파라미터:
      - `maasUrl` (필수): MAAS 서버 URL
      - `apiKey` (필수): API 키
      - `powerType` (필수): 전원 타입 (manual, ipmi)
      - **IPMI 파라미터** (powerType이 "ipmi"일 때만):
        - `powerDriver` (선택): Power Driver
        - `powerBootType` (선택): Power Boot Type
        - `powerIpAddress` (선택): IP Address
        - `powerUser` (선택): Power User
        - `powerPassword` (선택): Power Password (비워두면 기존 값 유지)
        - `powerKgBmcKey` (선택): K_g BMC key
        - `cipherSuiteId` (선택): Cipher Suite ID
        - `privilegeLevel` (선택): Privilege Level
        - `workaroundFlags` (선택): Workaround Flags (쉼표로 구분된 문자열)
        - `powerMac` (선택): Power MAC
    - 백엔드 처리:
      - 프론트엔드에서 받은 파라미터를 `MultiValueMap<String, String>`으로 변환
      - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
      - IPMI 파라미터는 `power_parameters_` 접두사를 붙여서 전송 (Add Machine과 동일)
      - 빈 값 필드는 form data에 포함하지 않음
    - MAAS API: `PUT /MAAS/api/2.0/machines/{systemId}/`
    - 응답:
    ```json
    {
      "success": true,
      "data": { /* 업데이트된 머신 정보 */ }
    }
    ```
  - **GET `/api/events/op-query`**: MAAS 이벤트 조회
    - 요청 파라미터: `maasUrl`, `apiKey`
    - 응답: MAAS 서버의 모든 이벤트 목록 (`{count: 100, events: [...]}` 형식)
    - 프론트엔드에서 `node` 필드가 현재 머신의 `system_id`와 일치하는 이벤트만 필터링
- **주요 코드 파일**:
  - `MachinesTab.vue`: 
    - `showMachineDetails()`: 머신 상세 정보 모달 열기
    - `closeMachineDetailsModal()`: 모달 닫기 (Power Type 변경 시에만 머신 목록 자동 리로드)
    - `loadMachineEvents()`: MAAS 이벤트 로드 및 필터링
    - `loadMachinePowerParameters()`: IPMI Power 파라미터 로드
    - `startEditingPowerParameters()`: Power 파라미터 편집 모드 시작
    - `cancelEditingPowerParameters()`: Power 파라미터 편집 취소
    - `savePowerParameters()`: Power 파라미터 저장
    - `formatPowerDriver()`, `formatPowerBootType()`, `formatCipherSuiteId()`, `formatPrivilegeLevel()`, `formatWorkaroundFlags()`: IPMI 파라미터 포맷팅 함수
    - `getUbuntuVersionFromDistroSeries()`: distro_series를 Ubuntu 버전으로 변환
    - `calculateStorageFromBlockDevices()`: Block Devices에서 총 스토리지 계산
    - `formatMemoryBytes()`: 메모리 포맷팅 (bytes → KB/MB/GB/TB)
    - 탭 전환 로직 (`activeDetailsTab`)
    - `machineEvents`, `loadingEvents`: 이벤트 상태 관리
    - `powerParameters`, `loadingPowerParameters`, `powerParametersError`: Power 파라미터 상태 관리
    - `isEditingPowerParameters`, `editingPowerParameters`, `savingPowerParameters`: 편집 상태 관리
    - `powerTypeChanged`: Power Type 변경 추적 (Detail 창 닫을 때 머신 목록 리로드용)
  - `MaasController.getMachine()`: 머신 상세 정보 API 엔드포인트
  - `MaasController.getMachineBlockDevices()`: Block Devices API 엔드포인트
  - `MaasController.getMachinePowerParameters()`: IPMI Power 파라미터 조회 API 엔드포인트
  - `MaasController.updateMachinePowerParameters()`: Power 파라미터 업데이트 API 엔드포인트
  - `MaasController.getEvents()`: MAAS 이벤트 조회 API 엔드포인트
  - `MaasApiService.getMachine()`: MAAS API 호출
  - `MaasApiService.getMachineBlockDevices()`: Block Devices API 호출
  - `MaasApiService.getMachinePowerParameters()`: IPMI Power 파라미터 API 호출 (`/MAAS/api/2.0/machines/{systemId}/op-power_parameters`)
  - `MaasApiService.updateMachinePowerParameters()`: Power 파라미터 업데이트 API 호출 (`PUT /MAAS/api/2.0/machines/{systemId}/`)
  - `MaasApiService.getEvents()`: MAAS Events API 호출 (`/MAAS/api/2.0/events/op-query`)
  - `MaasApiService.createPowerParametersFormData()`: Power 파라미터용 Form Data 생성
- **내부 로직 흐름**:
  1. FQDN 컬럼의 호스트명 클릭
  2. 머신 상세 정보 모달 열기 (기본적으로 Overview 탭 표시)
  3. 백엔드 `/api/machines/{systemId}` GET 요청으로 머신 상세 정보 로드
  4. 백엔드 `/api/machines/{systemId}/block-devices` GET 요청으로 Block Devices 정보 로드
  5. Power Type이 "ipmi"인 경우: 백엔드 `/api/machines/{systemId}/power-parameters` GET 요청으로 IPMI 파라미터 로드
  6. 백엔드 `/api/events/op-query` GET 요청으로 MAAS 이벤트 로드
  7. 이벤트 필터링: `node` 필드가 현재 머신의 `system_id`와 일치하는 이벤트만 추출
  8. 이벤트 정렬: `created` 필드 기준으로 최신순 정렬
  9. 탭 전환 시 해당 탭의 정보 표시
  10. Power 탭에서 Edit 버튼 클릭 시 편집 모드로 전환
  11. Power Type 변경 시 IPMI Configuration 필드 동적 표시/숨김
  12. Save 버튼 클릭 시 백엔드 `/api/machines/{systemId}/power-parameters` PUT 요청으로 저장
  13. 저장 성공 후 편집 모드 종료 및 데이터 다시 로드
  14. Power Type이 변경된 경우 `powerTypeChanged` 플래그 설정
  15. 모달 닫을 때 Power Type 변경 여부 확인 후 머신 목록 자동 리로드
  16. 모달 헤더 드래그로 위치 이동 가능 (브라우저 영역 내에서만)
- **UI/UX 특징**:
  - 모달 헤더를 드래그하여 브라우저 영역 내에서 위치 이동 가능
  - 탭 구조로 정보를 체계적으로 분류
  - 로딩 상태 및 에러 처리
    - 로딩 중에도 탭이 표시되어 팝업 크기가 일정하게 유지됨
    - 로딩/에러 메시지는 탭 컨텐츠 영역에 표시
  - 고정된 팝업 크기: 모든 탭에서 동일한 크기 유지 (600px 높이)
  - 스크롤 처리: 
    - Network 탭, Events 탭, Power 탭(편집 모드)은 스크롤 가능 (내용이 많을 경우)
    - 다른 탭에서는 스크롤바가 나타나지 않음
    - 스크롤바 하단 화살표가 완전히 보이도록 flexbox 구조로 높이 계산 최적화
    - Power 탭 편집 모드에서 편집 폼 내용이 많을 경우 내부 스크롤 가능 (부모 모달 스크롤 방지)
  - 반응형 디자인 (모바일 대응)
  - OS 버전 표시: Ubuntu의 경우 distro_series를 버전으로 변환하여 표시
    - xenial → 16.04 LTS
    - bionic → 18.04 LTS
    - focal → 20.04 LTS
    - jammy → 22.04 LTS
    - noble → 24.04 LTS
  - Network 탭 특화 기능:
    - 팝업 가로 크기 확대 (최대 1400px)로 여러 인터페이스를 가로로 배치
    - 한 줄에 4개의 인터페이스 항목 표시 (4개 초과 시 다음 줄)
    - 각 인터페이스 항목의 높이 자동 통일 (가장 긴 항목 기준)
    - IP Address와 Subnet을 같은 레벨의 독립적인 정보로 표시
    - 서브넷 정보 표시 형식: "Subnet: 서브넷정보" (명확한 라벨 사용)
  - Power 탭 특화 기능:
    - Power Type 필드를 상단에 독립적으로 배치
    - Power Type에 따라 IPMI Configuration 필드 동적 표시/숨김
    - 편집 모드에서 3열 그리드 레이아웃 (반응형: 2열 → 1열)
    - Workaround Flags 체크박스는 3열 그리드 (반응형: 2열 → 1열)
    - 편집 폼 내용이 많을 경우 내부 스크롤 가능 (부모 모달 스크롤 방지)
    - Edit/Cancel/Save 버튼은 항상 하단에 고정 표시
    - Power Type 변경 시 Detail 창 닫을 때 머신 목록 자동 리로드
- **에러 처리**: API 오류 시 에러 메시지 표시
- **성능 최적화**:
  - Block Devices 정보는 별도 API로 비동기 로드
  - Block Devices 로드 실패 시에도 기본 정보는 표시

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
| **Deployed** | 연한 녹색 (`btn-success-light`) | "Commission" | 활성화 | 재커미셔닝 (확인 메시지 표시), 또는 Release 버튼 표시 |
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
- **OS 선택**: Deploy 버튼 클릭 시 OS 선택 드롭다운 메뉴 표시
  - Deployable OS 목록은 최초 1회만 로드
  - 각 OS별 최신 release가 기본값으로 자동 선택됨
  - OS 목록은 OS 이름별로 그룹화되고, release는 최신순으로 정렬
- **OS 버전 표시**: Deploying 상태일 때 STATUS 컬럼에 배포 중인 OS 버전 표시
  - 형식: "OS 버전" (예: "Ubuntu 24.04")
  - 버전은 숫자만 표시 (LTS 등 제외)
  - `deployingOS`와 `deployingRelease` 정보 사용
  - Ubuntu release 매핑: xenial → 16.04, bionic → 18.04, focal → 20.04, jammy → 22.04, noble → 24.04
- **로딩 스피너**: 진행 중인 상태(Commissioning, Deploying 등)일 때 STATUS 컬럼에 회전하는 스피너 아이콘 표시
  - 스피너는 상태 배지 앞에 위치
  - CSS 애니메이션으로 1초에 한 바퀴 회전
  - 스피너 크기: 12px × 12px
  - 스피너 호 길이: 원의 약 3/4 정도 (border-top, border-right, border-bottom 색상)
  - 상태별 스피너 색상:
    - Commissioning: 주황색 (#f57c00)
    - Deploying: 청록색 (#00695c)
    - 기타 진행 중 상태: 기본 주황색
  - `isStatusInProgress()` 함수로 진행 중 상태 판별

#### Release 버튼 동작

| 머신 상태 | 버튼 표시 | 버튼 색상 | 활성화 여부 | 기능 |
|---------|---------|---------|-----------|------|
| **Failed Deployment** | 표시 | Release 스타일 (`btn-release`) | 릴리스 중이면 비활성화 | 머신 릴리스 (force=true) |
| **Failed Disk Erasing** | 표시 | Release 스타일 (`btn-release`) | 릴리스 중이면 비활성화 | 머신 릴리스 (force=true) |
| **Deployed** | 표시 | Release 스타일 (`btn-release`) | 릴리스 중이면 비활성화 | 머신 릴리스 (force=true) |
| **Allocated** | 표시 | Release 스타일 (`btn-release`) | 릴리스 중이면 비활성화 | 머신 릴리스 (force=true) |
| **그 외 모든 상태** | 숨김 | - | - | Commission 버튼이 표시됨 |

**특수 동작**:
- Failed Deployment, Failed Disk Erasing, Deployed, 또는 Allocated 상태에서 Release 버튼이 Commission 버튼 대신 표시됨
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
  - `showDeployModal()`: Deploy 모달 표시
  - `closeDeployModal()`: Deploy 모달 닫기
  - `startDeployFromModal()`: 모달에서 배포 시작
  - `loadDeployableOS()`: Deployable OS 목록 로드
  - `loadCloudConfigTemplates()`: Cloud-Config 템플릿 목록 로드
  - `matchedTemplates`: 머신 태그와 매칭되는 템플릿 (computed)
  - `otherTemplates`: 매칭되지 않은 템플릿 (computed)
  - `getSelectedTemplateCloudConfig`: 선택된 템플릿의 Cloud-Config (computed)
  - `deployMachine()`: 머신 배포 (OS 파라미터 포함)
  - `formatOSName()`: OS 이름 포맷팅
  - `getOSSortKey()`: OS 정렬 키 계산
  - `getDeployingOSVersion()`: Deploying 상태일 때 OS 버전 표시
  - `isFailedDeployment()`: Failed Deployment 상태 확인
  - `togglePowerMenu()`: Power 드롭다운 메뉴 토글
  - `handlePowerAction()`: Power 액션 처리 (Turn on/Turn off)
  - `getMachineById()`: 머신 ID로 머신 객체 조회

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
    - DELETE /api/machines/{systemId}
    - GET /api/machines/{systemId}/block-devices
    - GET /api/fabrics
    - GET /api/deployable-os
    - GET /api/fabrics/{fabricId}/vlans
    - GET /api/subnets
    - PUT /api/machines/{systemId}/interfaces/{interfaceId}/vlan
    - POST /api/machines/{systemId}/interfaces/{interfaceId}/link-subnet
    - POST /api/machines/{systemId}/interfaces/{interfaceId}/unlink-subnet
    - GET /api/machines/{systemId}/power-parameters
    - PUT /api/machines/{systemId}/power-parameters
    - POST /api/machines/{systemId}/power-on
    - POST /api/machines/{systemId}/power-off
    - GET /api/machines/{systemId}/query-power-state
    - GET /api/test-connection
    - GET /api/tags
    - POST /api/tags
    - PUT /api/tags/{oldName}
    - DELETE /api/tags/{name}
    - GET /api/cloud-config-templates
    - POST /api/cloud-config-templates
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
  - `deployMachine()`: 배포 (OS, release, arch 파라미터 포함, distro_series 형식으로 변환)
  - `getBootSources()`: Boot sources 목록 조회
  - `getBootSourceSelections()`: Boot source selections 조회
  - `getAllDeployableOS()`: 모든 Deployable OS 목록 조회
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
  - `getMachinePowerParameters()`: IPMI Power 파라미터 조회 (`/MAAS/api/2.0/machines/{systemId}/op-power_parameters` 호출)
  - `updateMachinePowerParameters()`: Power 파라미터 업데이트 (`PUT /MAAS/api/2.0/machines/{systemId}/` 호출)
  - `createPowerParametersFormData()`: Power 파라미터용 Form Data 생성
  - `getEvents()`: MAAS 이벤트 조회 (`/MAAS/api/2.0/events/op-query` 호출)
  - `powerOnMachine()`: 머신 전원 켜기
  - `powerOffMachine()`: 머신 전원 끄기
  - `queryPowerState()`: 머신 전원 상태 조회

##### MaasAuthService
- **역할**: OAuth 1.0 인증 헤더 생성
- **주요 메서드**:
  - `generateAuthHeader(apiKey)`: OAuth 헤더 생성
  - `isValidApiKey(apiKey)`: API 키 형식 검증
- **OAuth 1.0 인증 헤더 구현 상세**:
  - **OAuth 버전**: 1.0
  - **서명 방식**: PLAINTEXT
  - **API 키 형식**: `consumer_key:token:token_secret` (콜론으로 구분된 3개 파트)
  - **헤더 생성 과정**:
    1. API 키 파싱: 콜론(`:`)으로 분리하여 `consumerKey`, `token`, `tokenSecret` 추출
    2. Nonce 생성: UUID v4를 사용하여 매 요청마다 고유한 nonce 생성
    3. Timestamp 생성: 현재 시간을 초 단위로 변환 (`System.currentTimeMillis() / 1000`)
    4. 서명 생성: PLAINTEXT 방식으로 `"&" + tokenSecret` 형식의 서명 생성
    5. 헤더 문자열 생성: 다음 형식으로 OAuth 헤더 생성
       ```
       OAuth oauth_version="1.0", 
            oauth_signature_method="PLAINTEXT", 
            oauth_consumer_key="{consumerKey}", 
            oauth_token="{token}", 
            oauth_signature="&{tokenSecret}", 
            oauth_nonce="{nonce}", 
            oauth_timestamp="{timestamp}"
       ```
  - **에러 처리**:
    - API 키가 null이거나 빈 문자열인 경우: `IllegalArgumentException` 발생
    - API 키 형식이 올바르지 않은 경우 (3개 파트가 아닌 경우): `IllegalArgumentException` 발생
  - **사용 위치**: 모든 MAAS REST API 호출 시 `Authorization` 헤더에 포함
    - 예: `webClient.get().header("Authorization", authHeader)`

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
- **파일 위치**: 프로젝트 루트 디렉토리의 `maas-ui-settings.json`

##### CloudConfigTemplateService
- **역할**: Cloud-Config 템플릿 파일 관리
- **주요 메서드**:
  - `loadTemplates()`: 템플릿 파일 로드 및 마이그레이션
  - `saveTemplates(templates)`: 템플릿 파일 저장 (base64 인코딩 자동 추가)
  - `migrateTemplates(templates)`: 기존 형식 마이그레이션 (tag → tags, base64 자동 생성)
- **파일 위치**: 프로젝트 루트 디렉토리의 `maas-cloud-config-templates.json`
- **마이그레이션 기능**:
  - 기존 단일 `tag` 필드를 `tags` 배열로 변환
  - `cloudConfigBase64` 필드가 없으면 자동 생성
  - 마이그레이션이 필요한 경우 자동으로 파일 업데이트

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
- **요청 형식**: **Form Data** (`application/x-www-form-urlencoded` 또는 `multipart/form-data`)
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `hostname` (선택): 호스트명
  - `architecture` (선택, 기본값: amd64): 아키텍처
  - `macAddresses` (필수): MAC 주소 (쉼표로 구분)
  - `powerType` (선택, 기본값: manual): 전원 타입 (manual, ipmi)
  - `commission` (선택, 기본값: false): 커미셔닝 여부
  - `description` (선택): 설명
  - **IPMI 파라미터** (powerType이 "ipmi"일 때만):
    - `powerDriver` (선택, 기본값: LAN_2_0): Power Driver
    - `powerBootType` (선택, 기본값: auto): Power Boot Type
    - `powerIpAddress` (선택): IP Address
    - `powerUser` (선택): Power User
    - `powerPassword` (선택): Power Password
    - `powerKgBmcKey` (선택): K_g BMC key
    - `cipherSuiteId` (선택, 기본값: 3): Cipher Suite ID
    - `privilegeLevel` (선택, 기본값: OPERATOR): Privilege Level
    - `workaroundFlags` (선택, 기본값: opensesspriv): Workaround Flags (쉼표로 구분)
    - `powerMac` (선택): Power MAC
- **백엔드 처리**:
  - 프론트엔드에서 받은 파라미터를 `MultiValueMap<String, String>`으로 변환
  - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
  - IPMI 파라미터는 `power_parameters_` 접두사를 붙여서 전송:
    - `powerDriver` → `power_parameters_power_driver`
    - `powerBootType` → `power_parameters_power_boot_type`
    - `powerIpAddress` → `power_parameters_power_address`
    - `powerUser` → `power_parameters_power_user`
    - `powerPassword` → `power_parameters_power_pass`
    - `powerKgBmcKey` → `power_parameters_k_g`
    - `cipherSuiteId` → `power_parameters_cipher_suite_id`
    - `privilegeLevel` → `power_parameters_privilege_level`
    - `workaroundFlags` → `power_parameters_workaround_flags`
    - `powerMac` → `power_parameters_mac_address`
  - 빈 값 필드는 form data에 포함하지 않음
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
  - `os` (선택): OS 이름 (예: `ubuntu`)
  - `release` (선택): OS release (예: `noble`, `jammy`)
  - `arch` (선택): Architecture (예: `amd64`)
- **백엔드 처리**:
  - 프론트엔드에서 받은 `os`와 `release`를 조합하여 `distro_series` 생성
  - 형식: `os/release` (예: `ubuntu/noble`, `ubuntu/jammy`)
  - Form Data 생성 (`MultiValueMap<String, String>`)
  - `distro_series`와 `architecture`를 Form Data에 추가
  - `userdata`는 향후 추가 예정 (선택된 Cloud-Config 템플릿 또는 Custom Cloud-Config)
  - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
- **MAAS API**: `POST /MAAS/api/2.0/machines/{systemId}/op-deploy`
  - 요청 형식: **Form Data** (`application/x-www-form-urlencoded`)
  - Form Data 필드:
    - `distro_series` (선택): OS/release 형식 (예: `ubuntu/noble`)
    - `architecture` (선택): Architecture (예: `amd64`)
    - `userdata` (향후 추가): Cloud-Config YAML
- **응답**:
```json
{
  "success": true,
  "data": { /* 배포 결과 */ }
}
```

##### GET /api/deployable-os
- **설명**: Deploy 가능한 OS 목록 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **백엔드 처리**:
  - MAAS API `GET /MAAS/api/2.0/boot-sources/` 호출하여 boot source ID 목록 조회
  - 각 boot source에 대해 `GET /MAAS/api/2.0/boot-sources/{id}/selections/` 호출하여 OS 목록 조회
  - 중복 제거하여 고유한 OS 목록 반환
- **응답**:
```json
{
  "results": [
    {
      "os": "ubuntu",
      "release": "noble",
      "arches": ["amd64"]
    },
    {
      "os": "ubuntu",
      "release": "jammy",
      "arches": ["amd64"]
    }
  ],
  "count": 2
}
```
- **응답 필드**:
  - `results`: Deploy 가능한 OS 목록 (배열)
    - `os`: OS 이름 (예: `ubuntu`)
    - `release`: OS release (예: `noble`, `jammy`)
    - `arches`: 지원하는 architecture 목록 (예: `["amd64"]`)
  - `count`: OS 목록 개수

##### GET /api/tags
- **설명**: MAAS 태그 목록 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "name": "web",
      "definition": "",
      "comment": "Web server tag",
      "kernel_opts": ""
    }
  ]
}
```
- **MAAS API**: `GET /MAAS/api/2.0/tags/`
- **주요 코드 파일**:
  - `MaasController.getTags()`: API 엔드포인트
  - `MaasApiService.getAllTags()`: MAAS API 호출

##### POST /api/tags
- **설명**: 새 태그 생성
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `name` (필수): 태그 이름
  - `comment` (선택): 태그 설명
  - `definition` (선택): 태그 정의
  - `kernelOpts` (선택): 커널 옵션
- **요청 형식**: Form Data (`application/x-www-form-urlencoded`)
- **응답**:
```json
{
  "success": true,
  "data": {
    "name": "web",
    "comment": "Web server tag",
    "definition": "",
    "kernel_opts": ""
  }
}
```
- **MAAS API**: `POST /MAAS/api/2.0/tags/`
- **주요 코드 파일**:
  - `MaasController.createTag()`: API 엔드포인트
  - `MaasApiService.createTag()`: MAAS API 호출

##### PUT /api/tags/{oldName}
- **설명**: 태그 수정
- **요청 파라미터**:
  - `oldName` (경로 변수): 기존 태그 이름
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `name` (필수): 새 태그 이름
  - `comment` (선택): 태그 설명
  - `definition` (선택): 태그 정의
  - `kernelOpts` (선택): 커널 옵션
- **요청 형식**: Form Data (`application/x-www-form-urlencoded`)
- **응답**:
```json
{
  "success": true,
  "data": {
    "name": "web-server",
    "comment": "Web server tag",
    "definition": "",
    "kernel_opts": ""
  }
}
```
- **MAAS API**: `PUT /MAAS/api/2.0/tags/{name}/`
- **주요 코드 파일**:
  - `MaasController.updateTag()`: API 엔드포인트
  - `MaasApiService.updateTag()`: MAAS API 호출

##### DELETE /api/tags/{name}
- **설명**: 태그 삭제
- **요청 파라미터**:
  - `name` (경로 변수): 태그 이름
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
  - 성공 시: HTTP 200 OK
```json
{
  "success": true,
  "message": "Tag deleted successfully"
}
```
- **MAAS API**: `DELETE /MAAS/api/2.0/tags/{name}/`
- **MAAS 응답**: HTTP 204 NO_CONTENT (본문 없음)
- **백엔드 처리**: `toBodilessEntity()`를 사용하여 204 응답 처리

##### GET /api/cloud-config-templates
- **설명**: Cloud-Config 템플릿 목록 조회
- **요청**: 없음
- **응답**:
```json
{
  "success": true,
  "templates": [
    {
      "id": "1765329925967",
      "name": "kolla",
      "tags": ["kolla-ansible"],
      "description": "",
      "cloudConfig": "#cloud-config\n\npackage_update: true\n...",
      "cloudConfigBase64": "I2Nsb3VkLWNvbmZpZwoKcGFja2FnZV91cGRhdGU6IHRydWUuLi4=",
      "createdAt": "2025-12-10T01:25:25.967Z",
      "updatedAt": "2025-12-10T01:30:47.703Z"
    }
  ]
}
```
- **주요 코드 파일**:
  - `MaasController.getCloudConfigTemplates()`: API 엔드포인트
  - `CloudConfigTemplateService.loadTemplates()`: 파일 로드 및 마이그레이션
- **파일 위치**: 프로젝트 루트 디렉토리의 `maas-cloud-config-templates.json`

##### POST /api/cloud-config-templates
- **설명**: Cloud-Config 템플릿 저장 (생성 또는 수정)
- **요청 본문**: 템플릿 배열
```json
[
  {
    "id": "1765329925967",
    "name": "kolla",
    "tags": ["kolla-ansible"],
    "description": "",
    "cloudConfig": "#cloud-config\n\npackage_update: true\n...",
    "cloudConfigBase64": "I2Nsb3VkLWNvbmZpZwoKcGFja2FnZV91cGRhdGU6IHRydWUuLi4=",
    "createdAt": "2025-12-10T01:25:25.967Z",
    "updatedAt": "2025-12-10T01:30:47.703Z"
  }
]
```
- **응답**:
```json
{
  "success": true,
  "message": "Cloud-Config templates saved successfully"
}
```
- **주요 코드 파일**:
  - `MaasController.saveCloudConfigTemplates()`: API 엔드포인트
  - `CloudConfigTemplateService.saveTemplates()`: 파일 저장 (base64 인코딩 자동 추가)
- **파일 위치**: 프로젝트 루트 디렉토리의 `maas-cloud-config-templates.json`
- **자동 처리**:
  - `cloudConfigBase64` 필드가 없으면 `cloudConfig`를 base64 인코딩하여 자동 추가
  - 서버 재시작 후에도 데이터 유지
- **주요 코드 파일**:
  - `MaasController.deleteTag()`: API 엔드포인트
  - `MaasApiService.deleteTag()`: MAAS API 호출

##### DELETE /api/machines/{systemId}
- **설명**: 머신 삭제
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
  - 성공 시: HTTP 200 OK (응답 본문 없을 수 있음)
  - 실패 시: HTTP 500 Internal Server Error
```json
{
  "error": "Failed to delete machine"
}
```
- **MAAS API**: `DELETE /MAAS/api/2.0/machines/{systemId}/`

##### POST /api/machines/{systemId}/power-on
- **설명**: 머신의 전원을 켭니다
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "success": true,
  "power_state": "on"
}
```
- **에러 응답**:
```json
{
  "success": false,
  "error": "Failed to power on machine"
}
```
- **MAAS API**: `POST /MAAS/api/2.0/machines/{systemId}/op-power_on`
- **타임아웃**: 30초
- **주요 코드 파일**:
  - `MaasController.powerOnMachine()`: API 엔드포인트
  - `MaasApiService.powerOnMachine()`: MAAS API 호출

##### POST /api/machines/{systemId}/power-off
- **설명**: 머신의 전원을 끕니다
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "success": true,
  "power_state": "off"
}
```
- **에러 응답**:
```json
{
  "success": false,
  "error": "Failed to power off machine"
}
```
- **MAAS API**: `POST /MAAS/api/2.0/machines/{systemId}/op-power_off`
- **타임아웃**: 30초
- **주요 코드 파일**:
  - `MaasController.powerOffMachine()`: API 엔드포인트
  - `MaasApiService.powerOffMachine()`: MAAS API 호출

##### GET /api/machines/{systemId}/query-power-state
- **설명**: 머신의 전원 상태를 조회합니다
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "success": true,
  "state": "off"
}
```
- **에러 응답**:
```json
{
  "success": false,
  "error": "Failed to query power state"
}
```
- **MAAS API**: `GET /MAAS/api/2.0/machines/{systemId}/op-query_power_state`
- **타임아웃**: 30초
- **주요 코드 파일**:
  - `MaasController.queryPowerState()`: API 엔드포인트
  - `MaasApiService.queryPowerState()`: MAAS API 호출
- **동작**:
  - 전원 상태를 조회만 하고 변경하지 않음
  - 응답의 `state` 값(on, off, unknown 등)을 반환
  - 프론트엔드에서 응답의 `state` 값으로 머신의 `power_state`만 업데이트

##### GET /api/machines/{systemId}/power-parameters
- **설명**: IPMI Power 파라미터 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "power_driver": "LAN_2_0",
  "power_boot_type": "auto",
  "power_address": "192.168.1.100",
  "power_user": "admin",
  "power_pass": "password",
  "k_g": "",
  "cipher_suite_id": 3,
  "privilege_level": "OPERATOR",
  "workaround_flags": ["opensesspriv"],
  "mac_address": "08:00:27:11:34:26"
}
```
- **MAAS API**: `GET /MAAS/api/2.0/machines/{systemId}/op-power_parameters`
- **참고**: power_type이 "ipmi"인 경우에만 호출

##### PUT /api/machines/{systemId}/power-parameters
- **설명**: Power 파라미터 업데이트
- **요청 형식**: **Form Data** (`application/x-www-form-urlencoded`)
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `powerType` (필수): 전원 타입 (manual, ipmi)
  - **IPMI 파라미터** (powerType이 "ipmi"일 때만):
    - `powerDriver` (선택): Power Driver
    - `powerBootType` (선택): Power Boot Type
    - `powerIpAddress` (선택): IP Address
    - `powerUser` (선택): Power User
    - `powerPassword` (선택): Power Password (비워두면 기존 값 유지)
    - `powerKgBmcKey` (선택): K_g BMC key
    - `cipherSuiteId` (선택): Cipher Suite ID
    - `privilegeLevel` (선택): Privilege Level
    - `workaroundFlags` (선택): Workaround Flags (쉼표로 구분된 문자열)
    - `powerMac` (선택): Power MAC
- **백엔드 처리**:
  - 프론트엔드에서 받은 파라미터를 `MultiValueMap<String, String>`으로 변환
  - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
  - IPMI 파라미터는 `power_parameters_` 접두사를 붙여서 전송:
    - `powerDriver` → `power_parameters_power_driver`
    - `powerBootType` → `power_parameters_power_boot_type`
    - `powerIpAddress` → `power_parameters_power_address`
    - `powerUser` → `power_parameters_power_user`
    - `powerPassword` → `power_parameters_power_pass`
    - `powerKgBmcKey` → `power_parameters_k_g`
    - `cipherSuiteId` → `power_parameters_cipher_suite_id`
    - `privilegeLevel` → `power_parameters_privilege_level`
    - `workaroundFlags` → `power_parameters_workaround_flags`
    - `powerMac` → `power_parameters_mac_address`
  - 빈 값 필드는 form data에 포함하지 않음
- **응답**:
```json
{
  "success": true,
  "data": { /* 업데이트된 머신 정보 */ }
}
```
- **MAAS API**: `PUT /MAAS/api/2.0/machines/{systemId}/`

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

##### GET /api/events/op-query
- **설명**: MAAS 서버의 모든 이벤트 조회
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
- **응답**:
```json
{
  "results": [
    {
      "username": "unknown",
      "node": "hh66wc",
      "hostname": "maas",
      "id": 1572,
      "level": "INFO",
      "created": "Thu, 20 Nov. 2025 00:40:58",
      "type": "Ready",
      "description": ""
    }
  ]
}
```
- **MAAS API**: `GET /MAAS/api/2.0/events/op-query`
- **응답 형식**: `{count: 100, events: [...]}` 형식으로 반환되며, 백엔드에서 `events` 배열을 추출하여 프론트엔드로 전달
- **프론트엔드 처리**: 
  - `node` 필드가 현재 머신의 `system_id`와 일치하는 이벤트만 필터링
  - `created` 필드 기준으로 최신순 정렬 (내림차순)

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
- **설명**: 인터페이스의 VLAN 업데이트 또는 삭제
- **요청 형식**: **Form Data** (`application/x-www-form-urlencoded`)
- **VLAN 삭제**: vlanId가 빈 문자열이거나 null이면 vlan=""로 설정하여 VLAN 연결 제거
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `vlanId` (필수): VLAN ID
- **백엔드 처리**:
  - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
  - MAAS API: `PUT /MAAS/api/2.0/nodes/{systemId}/interfaces/{interfaceId}/` with form data `vlan={vlanId}`
- **응답**:
```json
{
  "success": true,
  "data": { /* 업데이트된 인터페이스 정보 */ }
}
```

##### POST /api/machines/{systemId}/interfaces/{interfaceId}/link-subnet
- **설명**: 인터페이스에 IP 주소 링크 (Primary 또는 Secondary)
- **요청 형식**: **Form Data** (`application/x-www-form-urlencoded`)
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `subnetId` (필수): Subnet ID
  - `ipAddress` (선택): IP 주소 (null이면 AUTO 모드)
- **백엔드 처리**:
  - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
  - MAAS API: `POST /MAAS/api/2.0/nodes/{systemId}/interfaces/{interfaceId}/op-link_subnet` with form data
- **응답**:
```json
{
  "success": true,
  "data": { /* 링크된 IP 주소 정보 */ }
}
```

##### POST /api/machines/{systemId}/interfaces/{interfaceId}/unlink-subnet
- **설명**: 인터페이스에서 IP 주소 링크 삭제
- **요청 형식**: **Form Data** (`application/x-www-form-urlencoded`)
- **요청 파라미터**:
  - `maasUrl` (필수): MAAS 서버 URL
  - `apiKey` (필수): API 키
  - `linkId` (필수): 링크 ID
- **백엔드 처리**:
  - `BodyInserters.fromFormData()`를 사용하여 MAAS API로 전송
  - MAAS API: `POST /MAAS/api/2.0/nodes/{systemId}/interfaces/{interfaceId}/op-unlink_subnet` with form data
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
  - `totalMachines`: 전체 머신 수 (null일 경우 "-" 표시)
  - `commissionedMachines`: 커미셔닝된 머신 수 (null일 경우 "-" 표시)
  - `deployedMachines`: 배포된 머신 수 (null일 경우 "-" 표시)
  - `deployableOSCount`: Deploy 가능한 OS 수 (null일 경우 "-" 표시)
  - `loading`: 로딩 상태
  - `error`: 에러 메시지
- **메서드**:
  - `loadMachineStats()`: 통계 데이터 로드
  - `loadDeployableOSCount()`: Deploy 가능한 OS 수 로드

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
  - `hoveredPowerMachine`: Power 컬럼 호버 상태
  - `openPowerMenu`: 열린 Power 메뉴 ID
  - `powerMenuPosition`: Power 메뉴 위치
  - `hoveredDeployMachine`: Deploy 버튼 호버 상태
  - `openDeployMenu`: 열린 Deploy OS 선택 메뉴 ID
  - `deployMenuPosition`: Deploy OS 선택 메뉴 위치
  - `deployableOSList`: Deployable OS 목록
  - `loadingDeployableOS`: Deployable OS 로딩 상태
- **메서드**:
  - `loadMachines()`: 머신 목록 로드
  - `showAddMachineModal()`: 머신 추가 모달 표시
  - `addMachine()`: 머신 추가
  - `handleCommissionButtonClick()`: 커미셔닝 버튼 클릭 처리
  - `commissionMachine()`: 머신 커미셔닝
  - `abortCommissioning()`: 커미셔닝 중단
  - `showNetworkModal()`: 네트워크 모달 표시
  - `handleDeployButtonClick()`: 배포 버튼 클릭 처리
  - `toggleDeployMenu()`: Deploy OS 선택 드롭다운 메뉴 토글
  - `loadDeployableOS()`: Deployable OS 목록 로드
  - `selectDeployOS()`: OS 선택 및 배포 시작
  - `deployMachine()`: 머신 배포 (OS 파라미터 포함)
  - `getDeployingOSVersion()`: Deploying 상태일 때 OS 버전 표시
  - `releaseMachine()`: 머신 릴리스
  - `togglePowerMenu()`: Power 드롭다운 메뉴 토글
  - `handlePowerAction()`: Power 액션 처리 (Turn on/Turn off)
  - `handleCheckPower()`: Power 상태 조회 처리 (Check power)
  - `getMachineById()`: 머신 ID로 머신 객체 조회
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
  - **인증 헤더 형식**: `OAuth oauth_version="1.0", oauth_signature_method="PLAINTEXT", oauth_consumer_key="...", oauth_token="...", oauth_signature="&...", oauth_nonce="...", oauth_timestamp="..."`
  - **서명 방식**: PLAINTEXT (consumer secret과 token secret을 `&`로 연결)
  - **Nonce**: 매 요청마다 UUID v4로 생성되는 고유 값
  - **Timestamp**: 요청 시점의 Unix timestamp (초 단위)
  - **API 키 형식**: `consumer_key:token:token_secret` (Settings에서 입력)
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

#### maas-cloud-config-templates.json
```json
[
  {
    "id": "1765329925967",
    "name": "kolla",
    "tags": ["kolla-ansible"],
    "description": "",
    "cloudConfig": "#cloud-config\n\npackage_update: true\n...",
    "cloudConfigBase64": "I2Nsb3VkLWNvbmZpZwoKcGFja2FnZV91cGRhdGU6IHRydWUuLi4=",
    "createdAt": "2025-12-10T01:25:25.967Z",
    "updatedAt": "2025-12-10T01:30:47.703Z"
  }
]
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
- **레이아웃**: 카드 그리드 (첫 번째 줄: 3열, 두 번째 줄: 1열)
- **컴포넌트**:
  - Stat Card (전체 머신 수) - 첫 번째 줄
  - Stat Card (커미셔닝된 머신 수) - 첫 번째 줄
  - Stat Card (배포된 머신 수) - 첫 번째 줄
  - Stat Card (Deploy 가능한 OS 수) - 두 번째 줄
- **스타일**: 그라데이션 배경, 호버 효과

#### Machines Tab
- **레이아웃**: 테이블 형태
- **컴포넌트**:
  - 헤더 (제목, 액션 바, 연결 상태 표시기)
    - 액션 바 (머신 선택 시 표시): Actions 드롭다운, Power 드롭다운, Delete 버튼, 선택된 머신 수
  - 검색 박스
  - 필터 버튼 (All, New, Commissioning, Ready, Allocated, Deployed, Failed)
  - "Add Machine" 버튼
  - 머신 테이블
    - 체크박스 컬럼 (전체 선택 체크박스 + 상태별 선택 드롭다운 아이콘)
    - FQDN 컬럼 (호스트명, MAC 주소, IP 주소)
    - Power 컬럼 (Power State, Power Type, Power Action 드롭다운)
    - Status 컬럼 (로딩 스피너, 상태 배지, 상태 메시지)
      - 진행 중인 상태(Commissioning, Deploying 등)일 때 회전하는 스피너 아이콘 표시
      - 스피너는 상태 배지 앞에 위치
      - 상태별 스피너 색상: Commissioning(주황색), Deploying(청록색)
    - Owner/Tags 컬럼
    - Pool, Zone, Fabric 컬럼
    - 하드웨어 정보 컬럼 (Cores, RAM, Disks, Storage)
    - Actions 컬럼 (상태별 동적 버튼: Commission/Release, Network, Deploy)
  - 페이지네이션
  - 모달:
    - Add Machine 모달
    - Network 설정 모달
    - Machine Details 모달 (드래그 가능)
    - Confirm/Alert 모달 (드래그 가능)
- **스타일**: 
  - 상태별 색상 코딩 (New: 파랑, Commissioning: 노랑, Ready: 초록, Deployed: 회색, Failed: 빨강)
  - 버튼 상태별 색상:
    - **Commission**: 녹색/연한 녹색 (활성화), 주황색 (Abort), 회색 (비활성화)
    - **Network**: 파랑 (저장 가능), 연한 파랑 (읽기 전용), 회색 (비활성화)
    - **Deploy**: 분홍색 (활성화), 주황색 (Abort), 회색 (비활성화)
    - **Release**: Release 전용 스타일 (Failed 상태)

#### Configuration Tab
- **레이아웃**: 섹션별 카드 형태
- **컴포넌트**:
  - **Images 섹션**: Deploy 가능한 OS 이미지 목록
    - OS, Release, Version, Architecture 컬럼으로 테이블 구성
    - Ubuntu release 버전 매핑 (xenial → 16.04 LTS 등)
    - 로딩/에러 상태 처리
  - **Tags 섹션**: MAAS 태그 목록
    - Name, Definition, Comment, Kernel Options 컬럼으로 테이블 구성
    - 태그 이름을 배지 스타일로 표시
    - 로딩/에러 상태 처리
  - **Cloud-Config Templates 섹션**: Cloud-Config 템플릿 관리
    - 템플릿 목록 테이블 (Name, Tags, Description, Actions)
    - "+ Add Template" 버튼
    - 템플릿 편집/삭제 기능
    - 템플릿 생성/편집 모달:
      - Name (필수), Tags (체크박스 선택), Description, Cloud-Config YAML (필수)
      - MAAS Tags 목록에서 체크박스로 선택
      - 태그 comment 표시
- **스타일**: 카드 형태의 섹션, 테이블, 모달

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

### 5.3 머신 상세 정보 모달 UI/UX 개선
- **고정된 팝업 크기**: 모든 탭에서 동일한 크기 유지 (500px 높이)
  - 로딩 중에도 탭이 표시되어 팝업 크기가 일정하게 유지됨
  - 탭 전환 시에도 팝업 크기 변화 없음
- **스크롤 처리**:
  - Network 탭을 제외한 다른 탭에서는 스크롤바가 나타나지 않음
  - Network 탭에서만 인터페이스가 많을 때 스크롤바 표시
- **Network 탭 레이아웃**:
  - 팝업 가로 크기 확대: 최대 1400px (기본 900px에서 확대)
  - 인터페이스 가로 배치: 한 줄에 4개의 인터페이스 항목 표시
  - 4개 초과 시 자동으로 다음 줄에 배치
  - 각 인터페이스 항목의 높이 자동 통일 (가장 긴 항목 기준)
  - 인터페이스 항목 너비: 약 23% (한 줄에 4개 배치)
  - 정보 항목 간 세로 간격: 1rem (시각적으로 여유있는 레이아웃)
- **Network 탭 정보 표시**:
  - IP Address와 Subnet을 같은 레벨의 독립적인 정보로 표시
  - 서브넷 정보 표시 형식: "Subnet: 서브넷정보" (명확한 라벨 사용)
  - 각 IP 주소마다 별도의 info-row로 표시

### 5.4 모달 드래그 기능
- **설명**: 모든 모달 팝업은 헤더를 드래그하여 브라우저 영역 내에서 위치 이동이 가능합니다.
- **적용 모달**:
  - **Machine Details 모달**: 머신 상세 정보 표시 모달
  - **Confirm/Alert 모달**: 확인 및 알림 메시지 모달 (커스텀 모달)
  - **Network 설정 모달**: 네트워크 설정 변경 모달
  - **Deploy 모달**: OS 선택 및 Cloud-Config 설정 모달
  - **Tag Information 모달**: 태그 생성/수정/삭제 모달
  - **Cloud-Config Template 모달**: 템플릿 생성/수정 모달
- **동작 방식**:
  - 모달 헤더에서 마우스를 누르고 드래그하면 모달이 이동
  - 드래그 중 커서가 `grab` → `grabbing`으로 변경
  - 모달이 브라우저 뷰포트 경계를 벗어나지 않도록 제한
  - 모달 닫을 때 위치가 자동으로 초기화됨
  - 모달 바깥쪽 클릭 시 모달이 닫히지 않음 (확인/취소 버튼으로만 닫힘)
- **기술 구현**:
  - `mousedown` 이벤트로 드래그 시작
  - `mousemove` 이벤트로 실시간 위치 업데이트
  - `mouseup` 이벤트로 드래그 종료
  - 전역 이벤트 리스너로 모달 밖에서도 드래그 가능
  - `position: fixed`로 모달 위치 제어
  - 뷰포트 경계 체크로 모달이 화면 밖으로 나가지 않도록 제한
- **사용자 경험**:
  - 드래그 중 텍스트 선택 방지 (`user-select: none`)
  - 드래그 중 전환 효과 비활성화 (`transition: none`)
  - 직관적인 드래그 인터페이스
  - 각 모달은 독립적인 위치 상태 관리
  - 모달 바깥쪽 클릭으로 실수로 닫히는 것을 방지
- **주요 코드 파일**:
  - `MachinesTab.vue`:
    - `startDragModal()`, `onDragModal()`, `stopDragModal()`: Machine Details 모달 드래그
    - `startDragConfirmModal()`, `onDragConfirmModal()`, `stopDragConfirmModal()`: Confirm/Alert 모달 드래그
    - `startDragNetworkModal()`, `onDragNetworkModal()`, `stopDragNetworkModal()`: Network 모달 드래그
    - `startDragDeployModal()`, `onDragDeployModal()`, `stopDragDeployModal()`: Deploy 모달 드래그
    - 각 모달별 독립적인 위치 상태 (`modalPosition`, `confirmModalPosition`, `networkModalPosition`, `deployModalPosition`)
  - `ConfigurationTab.vue`:
    - `startDragTagModal()`, `onDragTagModal()`, `stopDragTagModal()`: Tag Information 모달 드래그
    - `startDragTemplateModal()`, `onDragTemplateModal()`, `stopDragTemplateModal()`: Cloud-Config Template 모달 드래그
    - `startDragConfirmModal()`, `onDragConfirmModal()`, `stopDragConfirmModal()`: Confirm/Alert 모달 드래그
    - 각 모달별 독립적인 위치 상태 (`tagModalPosition`, `templateModalPosition`, `confirmModalPosition`)

### 5.4 CSS/스타일 프레임워크
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

### 5.5 주요 시각적 규칙

#### 상태 표시
- **New**: 파랑 배지
- **Commissioning**: 노랑 배지
- **Ready**: 초록 배지
- **Allocated**: 초록 배지
- **Deployed**: 회색 배지 (비활성화)
  - Ubuntu 머신의 경우 STATUS 메시지에 OS 버전 표시 (예: "Ubuntu 24.04 LTS")
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
- **Line 887**: `// TODO: Implement machine actions menu`
  - 머신 액션 메뉴 구현 필요

### 7.2 현재 미완성된 기능

#### 2. 머신 액션 메뉴
- **현재 상태**: 미구현
- **필요 작업**:
  - 드롭다운 메뉴로 추가 액션 제공
  - Tag 관리, Owner 변경, Zone/Pool 변경 등

#### 3. Power Action 기능 (Turn on/Turn off/Check power)
- **현재 상태**: ✅ 구현 완료
- **구현 내용**:
  - **백엔드 API 엔드포인트**:
    - `POST /api/machines/{systemId}/power-on`: 머신 전원 켜기
    - `POST /api/machines/{systemId}/power-off`: 머신 전원 끄기
    - `GET /api/machines/{systemId}/query-power-state`: 머신 전원 상태 조회
  - **백엔드 서비스 메서드**:
    - `MaasApiService.powerOnMachine()`: MAAS API `/MAAS/api/2.0/machines/{systemId}/op-power_on` 호출
    - `MaasApiService.powerOffMachine()`: MAAS API `/MAAS/api/2.0/machines/{systemId}/op-power_off` 호출
    - `MaasApiService.queryPowerState()`: MAAS API `/MAAS/api/2.0/machines/{systemId}/op-query_power_state` 호출 (GET 방식)
    - 타임아웃: 30초
  - **프론트엔드 구현**:
    - `handlePowerAction()`: 개별 머신 Power on/off API 호출
    - `handleCheckPower()`: 개별 머신 Power 상태 조회 API 호출 (다른 액션과 완전히 분리)
    - Power 상태에 따라 메뉴 항목 조건부 표시:
      - Power가 `off` 상태이면 "Turn on"만 표시
      - Power가 `on` 상태이면 "Turn off"만 표시
      - "Check power"는 항상 표시 (모든 Power 상태에서 사용 가능)
    - Power on/off 성공 시 WebSocket을 통해 상태 자동 업데이트 (페이지 리로드 없음)
    - Check power 성공 시 응답의 `state` 값으로 머신의 `power_state`만 업데이트 (다른 작업 수행 안 함)
  - **일괄 Power 작업**:
    - 액션 바의 Power 드롭다운 메뉴에서 일괄 Power on/off 지원
    - 선택된 머신들의 Power 상태에 따라 메뉴 활성화/비활성화:
      - 모두 `off` 상태 → "Turn on..."만 활성화, "Turn off..." 비활성화
      - 모두 `on` 상태 → "Turn off..."만 활성화, "Turn on..." 비활성화
      - 혼재 상태 → 둘 다 비활성화
    - `handleBulkPowerAction()`: 선택된 모든 머신에 대해 Power on/off API 호출
    - 결과 요약 표시 (성공/실패 개수)
    - WebSocket을 통해 상태 자동 업데이트 (페이지 리로드 없음)
    - WebSocket을 통해 상태 자동 업데이트 (페이지 리로드 없음)
  - **Check power 기능**:
    - MAAS에서 IPMI 상태 업데이트 주기가 길기 때문에 수동으로 전원 상태를 확인
    - 전원 상태를 조회만 하고 변경하지 않음
    - 응답의 `state` 값(on, off, unknown 등)으로 머신의 `power_state`만 업데이트
  - **Power 상태 실시간 업데이트**: WebSocket을 통해 자동 업데이트

#### 4. 배포 중단 (Abort Deploy) 기능
- **현재 상태**: ✅ 구현 완료
- **구현 내용**:
  - `MaasController.abortOperation()`: `/api/machines/{systemId}/abort` 엔드포인트 (Commissioning과 Deploying 모두 처리)
  - `MaasApiService.abortCommissioning()`: MAAS API `/MAAS/api/2.0/machines/{systemId}/op-abort` 호출
  - 타임아웃: 60초
  - **에러 처리**:
    - 503 Service Unavailable 또는 타임아웃 에러 발생 시:
      - "Abort operation may have timed out, but the operation might still be in progress. Refreshing machine status..." 메시지 표시
      - 2초 후 자동으로 머신 상태 재확인 (`loadMachines()` 호출)
  - **특수 동작**:
    - Failed Deployment 상태에서 Abort 시도 시: 자동으로 Release 작업으로 전환
    - Deploying 상태에서 Abort 시도 시: 정상적으로 Abort 처리

#### 5. 다중 머신 선택 액션
- **현재 상태**: 상태별 머신 선택 기능 및 일괄 액션 바 구현 완료, Delete 기능 완료, Power 기능 완료
- **구현 완료**:
  - ✅ 상태별 머신 일괄 선택/해제 기능
  - ✅ 전체 선택 체크박스와 상태별 선택 드롭다운 통합
  - ✅ 일괄 액션 바 (머신 선택 시 상단 표시)
  - ✅ 일괄 Delete 기능 (백엔드 API 연결 및 테스트 완료)
  - ✅ Actions 드롭다운 메뉴 (Commission, Allocate, Deploy, Release, Abort)
  - ✅ Power 드롭다운 메뉴 (개별 및 일괄 Power On/Off 기능 구현 완료)
- **일괄 Power 작업 기능 상세**:
  - **메뉴 활성화/비활성화 로직**:
    - `canBulkPowerOn()`: 선택된 모든 머신이 `off` 상태일 때만 `true`
    - `canBulkPowerOff()`: 선택된 모든 머신이 `on` 상태일 때만 `true`
    - 혼재 상태 (일부는 `on`, 일부는 `off`): 둘 다 비활성화
  - **작업 흐름**:
    1. 여러 머신 선택
    2. 액션 바의 "Power" 드롭다운 메뉴 클릭
    3. Power 상태에 따라 활성화된 메뉴 항목 선택 ("Turn on..." 또는 "Turn off...")
    4. 확인 메시지 표시 (`선택된 N개의 머신의 전원을 켜기/끄기 하시겠습니까?`)
    5. 확인 시 각 머신에 대해 순차적으로 Power on/off API 호출
    6. 결과 수집 및 요약 표시 (성공/실패 개수)
    7. 2초 후 머신 목록 자동 새로고침
  - **에러 처리**:
    - 각 머신별로 개별 에러 처리
    - 실패한 머신의 에러 메시지 수집
    - 전체 결과 요약 표시 (예: "Power on: 3개 성공, 1개 실패")
- **필요 작업**:
  - 일괄 Commission, Deploy, Release, Abort 기능 백엔드 API 연결

#### 6. 에러 처리 개선
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

