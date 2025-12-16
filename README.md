# MaaS UI Server

A modern web interface for MAAS (Metal as a Service) management, built with Vue.js 3 frontend and Spring Boot backend.

## 🚀 Features

- **Dashboard**: Real-time machine statistics and overview
- **Machines Management**: Comprehensive machine listing with detailed information
  - Real-time status updates via WebSocket
  - Machine commissioning, deployment, and release
  - Network configuration
  - Power management (Turn on/off, Check power)
  - Machine details modal with multiple tabs
  - Status-based machine selection
  - Loading spinner indicators for in-progress operations
- **Configuration**: Manage deployable OS images, tags, and Cloud-Config templates
  - Deployable OS images listing with deployed machine counts
  - Tag management (create, update, delete)
  - Cloud-Config template management with file-based storage
- **Settings**: MAAS server configuration and connection testing
- **Responsive Design**: Modern UI optimized for 90% browser width utilization
- **Real-time Data**: Live data from MAAS API with OAuth 1.0 authentication
- **WebSocket Support**: Real-time machine status updates without page refresh

## 🏗️ Architecture

### Frontend (Vue.js 3)
- **Framework**: Vue.js 3 with Composition API
- **Build Tool**: Webpack 5
- **Port**: 8080
- **Features**: 
  - Responsive table layout for machines
  - Real-time data fetching
  - Modern UI components
  - Custom modal dialogs (draggable, non-dismissible on outside click)
  - WebSocket integration for live updates
  - Local state management with composables

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.2.0
- **Java Version**: Java 17
- **Port**: 8081
- **Features**:
  - RESTful API endpoints
  - OAuth 1.0 authentication
  - MAAS API integration
  - Reactive programming with WebFlux
  - WebSocket server for real-time updates
  - File-based configuration storage (JSON)
  - Cloud-Config template management

## 📋 Prerequisites

- Java 17+
- Node.js 16+
- Maven 3.6+
- MAAS Server with API access

## 🛠️ Installation & Setup

### Backend Setup
```bash
cd backend/maas-ui-backend
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8081`

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:8080`

## 🌐 API Endpoints

### Dashboard
- `GET /api/dashboard/stats` - Machine statistics

### Machines
- `GET /api/machines` - Get all machines
- `GET /api/machines/{systemId}` - Get machine details
- `POST /api/machines` - Add new machine
- `DELETE /api/machines/{systemId}` - Delete machine
- `POST /api/machines/{systemId}/commission` - Commission machine
- `POST /api/machines/{systemId}/abort` - Abort commissioning
- `POST /api/machines/{systemId}/deploy` - Deploy machine
- `POST /api/machines/{systemId}/release` - Release machine
- `POST /api/machines/{systemId}/power-on` - Turn on machine
- `POST /api/machines/{systemId}/power-off` - Turn off machine
- `GET /api/machines/{systemId}/query-power-state` - Check power state
- `GET /api/machines/{systemId}/block-devices` - Get block devices
- `GET /api/machines/{systemId}/power-parameters` - Get power parameters
- `PUT /api/machines/{systemId}/power-parameters` - Update power parameters
- `GET /api/machines/{systemId}/interfaces` - Get network interfaces
- `PUT /api/machines/{systemId}/interfaces/{interfaceId}/vlan` - Update VLAN
- `POST /api/machines/{systemId}/interfaces/{interfaceId}/link-subnet` - Link subnet

### Configuration
- `GET /api/deployable-os` - Get deployable OS images
- `GET /api/tags` - Get all tags
- `POST /api/tags` - Create tag
- `PUT /api/tags/{name}` - Update tag
- `DELETE /api/tags/{name}` - Delete tag
- `GET /api/cloud-config-templates` - Get Cloud-Config templates
- `POST /api/cloud-config-templates` - Save Cloud-Config template

### Settings
- `GET /api/settings` - Get settings
- `POST /api/settings` - Save settings
- `GET /api/test-connection` - Test MAAS connection

### Health
- `GET /api/health` - Health check

## 🔧 Configuration

### Settings File
Settings are stored in `backend/maas-ui-backend/maas-ui-settings.json`:
- MAAS server URL
- API key
- Refresh interval
- Auto-refresh enabled

### Cloud-Config Templates
Cloud-Config templates are stored in `backend/maas-ui-backend/maas-cloud-config-templates.json`:
- Template name, description, tags
- Cloud-Config YAML content
- Base64 encoded Cloud-Config for deployment
- Created/updated timestamps

## 📱 Usage

1. Start both frontend and backend servers
2. Navigate to `http://localhost:8080`
3. Configure MAAS server settings in the Settings tab
4. View machine statistics in Dashboard tab
5. Manage machines in Machines tab:
   - Commission machines
   - Configure network interfaces
   - Deploy machines with OS and Cloud-Config templates
   - Manage power states
   - View detailed machine information
6. Manage configuration in Configuration tab:
   - View deployable OS images
   - Create, update, and delete tags
   - Manage Cloud-Config templates

## 🔐 Authentication

The application uses OAuth 1.0 authentication with MAAS servers. API keys should be in the format:
```
consumer_key:token:token_secret
```

## 📊 Machine Information Displayed

- **FQDN**: Fully qualified domain name with MAC and IP addresses
- **Power State**: Current power status with LED indicator
- **Status**: Machine lifecycle status with loading spinner for in-progress operations
  - Status badges with color coding
  - Status messages for detailed information
  - OS version display during deployment
- **Owner & Tags**: Machine ownership and tags
- **Pool & Zone**: Resource pool and zone information
- **Fabric**: Network fabric information
- **Hardware**: CPU cores, RAM, disk count, storage
- **Actions**: Context-aware action buttons (Commission, Release, Network, Deploy)

## 🔄 Real-time Updates

The application uses WebSocket for real-time machine status updates:
- Automatic reconnection on connection loss
- Heartbeat mechanism for connection health
- Broadcast of MAAS events to all connected clients
- No page refresh required for status updates

## 🚀 Development

### Project Structure
```
maas-ui-server/
├── backend/
│   └── maas-ui-backend/          # Spring Boot backend
│       ├── src/main/java/com/maas/ui/
│       │   ├── config/            # Configuration classes
│       │   │   ├── CorsConfig.java
│       │   │   └── WebSocketConfig.java
│       │   ├── controller/        # REST API controllers
│       │   │   └── MaasController.java
│       │   ├── handler/           # WebSocket handlers
│       │   │   └── ClientWebSocketHandler.java
│       │   ├── service/           # Business logic services
│       │   │   ├── MaasApiService.java
│       │   │   ├── MaasAuthService.java
│       │   │   ├── MaasWebSocketService.java
│       │   │   ├── SettingsService.java
│       │   │   └── CloudConfigTemplateService.java
│       │   └── MaasUiBackendApplication.java
│       ├── src/main/resources/
│       │   └── application.properties
│       ├── maas-ui-settings.json      # Settings file
│       ├── maas-cloud-config-templates.json  # Cloud-Config templates
│       └── pom.xml
├── frontend/                      # Vue.js 3 frontend
│   ├── src/
│   │   ├── components/            # Vue components
│   │   │   ├── DashboardTab.vue
│   │   │   ├── MachinesTab.vue
│   │   │   ├── ConfigurationTab.vue
│   │   │   └── SettingsTab.vue
│   │   ├── composables/           # Vue Composition API utilities
│   │   │   ├── useSettings.js
│   │   │   └── useWebSocket.js
│   │   ├── App.vue
│   │   └── main.js
│   ├── public/
│   │   └── index.html
│   ├── webpack.config.js
│   └── package.json
├── PRD.md                         # Product Requirements Document
└── README.md
```

### Key Technologies

**Frontend:**
- Vue.js 3 (Composition API)
- Axios (HTTP client)
- WebSocket API
- Webpack 5

**Backend:**
- Spring Boot 3.2.0
- Spring WebFlux (Reactive)
- Jetty WebSocket Client
- Jackson (JSON processing)

## 📝 License

This project is licensed under the MIT License.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 Support

For issues and questions, please create an issue in the GitHub repository.
