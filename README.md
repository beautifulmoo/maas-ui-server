# MaaS UI Server

A modern web interface for MAAS (Metal as a Service) management, built with Vue.js 3 frontend and Spring Boot backend.

## 🚀 Features

- **Dashboard**: Real-time machine statistics and overview
- **Machines Management**: Comprehensive machine listing with detailed information
- **Settings**: MAAS server configuration and connection testing
- **Responsive Design**: Modern UI optimized for 90% browser width utilization
- **Real-time Data**: Live data from MAAS API with OAuth 1.0 authentication

## 🏗️ Architecture

### Frontend (Vue.js 3)
- **Framework**: Vue.js 3 with Composition API
- **Build Tool**: Webpack 5
- **Port**: 8080
- **Features**: 
  - Responsive table layout for machines
  - Real-time data fetching
  - Modern UI components

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.2.0
- **Java Version**: Java 17
- **Port**: 8081
- **Features**:
  - RESTful API endpoints
  - OAuth 1.0 authentication
  - MAAS API integration
  - Reactive programming with WebFlux

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

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

## 🌐 API Endpoints

- `GET /api/health` - Health check
- `GET /api/dashboard/stats` - Machine statistics
- `GET /api/machines` - Machine list
- `GET /api/test-connection` - MAAS connection test

## 🔧 Configuration

Update the MAAS server URL and API key in the frontend components:
- `DashboardTab.vue`
- `MachinesTab.vue` 
- `SettingsTab.vue`

## 📱 Usage

1. Start both frontend and backend servers
2. Navigate to `http://localhost:8080`
3. Configure MAAS server settings in the Settings tab
4. View machine statistics in Dashboard tab
5. Manage machines in Machines tab

## 🔐 Authentication

The application uses OAuth 1.0 authentication with MAAS servers. API keys should be in the format:
`consumer_key:token:token_secret`

## 📊 Machine Information Displayed

- **FQDN**: Fully qualified domain name
- **Power State**: Current power status
- **Status**: Machine lifecycle status
- **Owner & Tags**: Machine ownership and tags
- **Pool & Zone**: Resource pool and zone information
- **Hardware**: CPU cores, RAM, disk count, storage
- **Network**: MAC addresses and IP addresses

## 🚀 Development

### Project Structure
```
maas-ui-server/
├── backend/
│   └── maas-ui-backend/
│       ├── src/main/java/com/maas/ui/
│       │   ├── controller/
│       │   ├── service/
│       │   └── MaasUiBackendApplication.java
│       └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── webpack.config.js
└── README.md
```

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
