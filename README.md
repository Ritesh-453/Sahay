# 🚗 RoadAssist — Roadside Breakdown Assistance System

> A smart platform connecting stranded drivers with nearby mechanics — fast, reliable, and trackable.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![Railway](https://img.shields.io/badge/Deployed-Railway-purple?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

**Live Demo:** [roadassist-production-dcb6.up.railway.app](https://roadassist-production-dcb6.up.railway.app)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Endpoints](#api-endpoints)
- [Deployment](#deployment)
- [Default Credentials](#default-credentials)
- [Roadmap](#roadmap)
- [Author](#author)

---

## 🎯 Overview

**RoadAssist** is a full-stack web application built as a college project for the IP (Internet Programming) subject in second year IT Engineering. It simulates a real-world roadside assistance platform that digitizes the entire breakdown-to-mechanic workflow.

### Why This Matters

In India, roadside breakdowns are extremely common but the assistance process is completely unorganized:

- 🚗 Drivers have no way to formally register a breakdown request
- 🔧 No tracking system to know if help is coming
- 📍 No way to share exact location with mechanics
- 📊 No data on common breakdown patterns for analysis

**RoadAssist solves this** by providing a structured digital platform for customers, mechanics (via admin), and management (via analytics).

---

## 💥 Problem Statement

### The Roadside Assistance Challenge

When a vehicle breaks down on an Indian road, the driver faces multiple problems:

**Critical Pain Points**

❌ **No Digital Record** — Breakdown requests are handled verbally with no tracking  
❌ **No Location Sharing** — Mechanics have to find customers without precise coordinates  
❌ **No Status Updates** — Customer has no idea when mechanic will arrive  
❌ **No Analytics** — Management cannot identify common breakdown patterns  
❌ **No Accountability** — No record of which mechanic handled which request  

### Our Solution

A web-based platform where:
- Customers submit breakdown requests with GPS location and vehicle details
- Admins assign mechanics with name, phone, ETA, and live location
- The entire workflow is tracked from Pending → Accepted → Completed
- Analytics dashboard provides insights on issue patterns and response metrics

---

## ✨ Key Features

### 👤 Customer Portal
- ✅ Register and login with password strength validation
- ✅ Submit breakdown requests with:
  - Indian vehicle number auto-formatting (`MH 12 AB 1234`)
  - 10 common issue types + custom description option
  - 10-digit Indian phone number validation
  - **Live GPS location** detection
  - **Choose on Map** — India-only interactive map picker
- ✅ View all submitted requests with real-time status
- ✅ View assigned mechanic details with their map location

### 🔧 Admin Dashboard
- ✅ Secure admin login (hidden access link on login page)
- ✅ Stats overview — Total, Pending, Accepted, Completed requests
- ✅ Accept requests by filling mechanic name, phone, ETA, and location
- ✅ Mark requests as Completed
- ✅ Navigate to Analytics in one click

### 📊 Analytics
- ✅ Filter requests by custom date and time range
- ✅ Detailed requests table with IST timestamps (24-hour format)
- ✅ **Status breakdown** doughnut chart (Pending / Accepted / Completed)
- ✅ **Issues breakdown** doughnut chart (Top 10 most common issues)
- ✅ **Download PDF Report** — auto-generated with summary stats and full data table

### 🎨 UI/UX
- ✅ Professional split-screen login page
- ✅ Fully mobile responsive across all pages
- ✅ RoadAssist `RA` logo visible on mobile screens
- ✅ India-only map with boundary rectangle and grey overlay outside
- ✅ 24-hour IST time format throughout

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (HTML/CSS/JS)                   │
│  index.html  •  customer.html  •  admin.html  •  analytics  │
│  Leaflet Maps  •  Chart.js  •  jsPDF                        │
└────────────────────┬────────────────────────────────────────┘
                     │ REST API (JSON)
┌────────────────────▼────────────────────────────────────────┐
│               SPRING BOOT BACKEND (Java 17)                 │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │    Auth      │  │   Request    │  │  Analytics   │       │
│  │ Controller   │  │  Controller  │  │  Endpoint    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │   AppUser    │  │  Service     │                         │
│  │    Model     │  │  Request     │                         │
│  └──────────────┘  └──────────────┘                         │
└────────────────────┬────────────────────────────────────────┘
                     │ JPA / Hibernate
┌────────────────────▼────────────────────────────────────────┐
│                  MySQL DATABASE                             │
│       app_user table  •  service_request table              │
└─────────────────────────────────────────────────────────────┘
```

### Request Lifecycle

```
Customer Submits → PENDING → Admin Accepts → ACCEPTED → Admin Completes → COMPLETED
                                   ↓
                        Mechanic Details Assigned
                        (Name, Phone, ETA, Location)
```

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3.2.5 |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA / Hibernate |
| Frontend | HTML5, CSS3, Vanilla JavaScript |
| Maps | Leaflet.js + OpenStreetMap |
| Charts | Chart.js (Doughnut) |
| PDF Export | jsPDF + jsPDF-AutoTable |
| Fonts | Google Fonts (Syne, DM Sans) |
| Deployment | Railway (Backend + MySQL) |
| Version Control | GitHub |

---

## 📁 Project Structure

```
roadassist/
├── src/
│   ├── main/
│   │   ├── java/com/example/roadassist/
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java           ← Login & Register APIs
│   │   │   │   └── ServiceRequestController.java ← Request CRUD & Analytics
│   │   │   ├── model/
│   │   │   │   ├── AppUser.java                  ← User entity
│   │   │   │   └── ServiceRequest.java           ← Breakdown request entity
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── ServiceRequestRepository.java
│   │   │   └── RoadassistApplication.java        ← Main entry point
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── index.html                    ← Login / Register page
│   │       │   ├── customer.html                 ← Customer dashboard
│   │       │   ├── admin.html                    ← Admin dashboard
│   │       │   └── analytics.html                ← Analytics & PDF reports
│   │       └── application.properties            ← DB config (not committed)
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0 running locally

### 1. Clone the repository

```bash
git clone https://github.com/Ritesh-453/roadassist.git
cd roadassist
```

### 2. Set up local database

Open MySQL and run:

```sql
CREATE DATABASE my_app_db;
```

### 3. Configure `application.properties`

Edit `src/main/resources/application.properties` for local development:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/my_app_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
server.port=8082
```

> ⚠️ Add `application.properties` to `.gitignore` to avoid exposing credentials.

### 4. Run the application

```bash
mvn spring-boot:run
```

### 5. Open in browser

```
http://localhost:8082
```

---

## 🔐 Environment Variables

For production deployment on Railway, set these variables on the app service:

| Variable | Value |
|----------|-------|
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.MySQL8Dialect` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| `PORT` | `8082` |

---

## 📡 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register new customer |
| `POST` | `/api/auth/login` | Login — returns role (CUSTOMER / ADMIN) |

### Service Requests

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/requests` | Get all requests (admin only) |
| `POST` | `/api/requests` | Submit new breakdown request |
| `GET` | `/api/requests/customer/{username}` | Get requests by customer |
| `GET` | `/api/requests/analytics?from=&to=` | Get requests in date range |
| `PATCH` | `/api/requests/{id}/accept` | Accept request & assign mechanic |
| `PATCH` | `/api/requests/{id}/status` | Update request status |

### Sample Request Body — Submit Breakdown

```json
{
  "customerName": "Ritesh Purohit",
  "vehicleNumber": "MH 48 AE 9587",
  "issue": "Tyre Puncture",
  "phone": "9876543210",
  "latitude": 19.4550,
  "longitude": 72.8014
}
```

### Sample Request Body — Accept & Assign Mechanic

```json
{
  "mechanicName": "Ramesh Kumar",
  "mechanicPhone": "9812345678",
  "estimatedArrival": "30 minutes",
  "mechanicLatitude": "19.4600",
  "mechanicLongitude": "72.8050"
}
```

---

## ☁️ Deployment

This project is deployed on **Railway** with automatic deploys from GitHub.

### Deploy Architecture

```
GitHub (main branch)
       ↓ auto-deploy trigger
Railway Build (Maven)
       ↓
Spring Boot App (Port 8082)
       ↓
Railway MySQL (Internal Network)
```

### Re-deploy

```bash
git add .
git commit -m "your message"
git push
```

Railway rebuilds and redeploys automatically within 2-3 minutes.

### Setting Up on Railway from Scratch

1. Create new Railway project
2. Add **GitHub repo** as a service
3. Add **MySQL** database service
4. Set environment variables on the app service (see above)
5. Generate domain → set target port to `8082`

---

## 🔑 Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin123` |
| Customer | Register via the app | Your chosen password |

> ⚠️ Change the default admin password before using in a real environment.

---

## 🗺️ Roadmap

### Completed ✅
- [x] Customer registration and login
- [x] Breakdown request submission with GPS location
- [x] Admin dashboard with mechanic assignment
- [x] Analytics with doughnut charts
- [x] PDF report download
- [x] Mobile responsive UI
- [x] Railway cloud deployment

### Planned 🚧
- [ ] SMS/Email notifications when mechanic is assigned
- [ ] Real mechanic accounts (not just admin-assigned)
- [ ] Live mechanic tracking on customer map
- [ ] Rating system for mechanics
- [ ] Multi-language support (Hindi, Marathi)
- [ ] Android/iOS mobile app

---

## 👨‍💻 Author

**Ritesh Purohit** — Second Year IT Engineering Student  
GitHub: [@Ritesh-453](https://github.com/Ritesh-453)

---

## 🙏 Acknowledgments

- **Spring Boot** — For making Java backend development fast and simple
- **Leaflet.js** — For the excellent open-source mapping library
- **Chart.js** — For beautiful and responsive charts
- **Railway** — For free and easy cloud deployment
- **OpenStreetMap** — For free map tiles

---

## 📄 License

This project is built for educational purposes as part of an IP (Internet Programming) college project.

---

*Built with ❤️ for IP College Project — Second Year IT Engineering*
