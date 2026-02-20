# 🎭 Event Seatmap Platform

A full-featured **event ticketing and seatmap management platform** built with **Angular**, **Spring Boot**, and **Java** as part of the **TU Wien – Software Engineering & Project (SEPR)** course.

The system combines a dynamic seatmap generator, a complete event backend, and a flexible data model for tickets, locations, artists, pricing, and seating.

---

## 🎨 UI Preview

![Seatmap Overview](docs/images/seatmap-variant1.png)
![Event Page](docs/images/event-page.png)

## 🚀 Features

### 🪑 Dynamic Seatmaps
- 4 predefined seatmap variants (theatre, concert hall, center stage, trapezoid)
- Fixed column numbering → removing seats never shifts the layout
- Supports:
  - Stage placement (TOP, BOTTOM, CENTER)
  - Custom rectangular stage boxes
  - Catwalks / T-shaped stages
  - Blocked rows & columns
  - Aisles and custom gaps per row

### 🎟 Ticket & Event Management
- Events automatically inherit the seatmap of their assigned location
- Dynamic generation of:
  - Tickets  
  - Reservations  
  - Invoices  
- Price logic based on location & sector
- Artists supported (bands or individuals)

### 🏟 Location System
Each location contains:
- Stage + StageBox coordinates  
- Sectors (A, B, C, Standing)  
- Pricing categories  
- Fully generated seating layout  

### 🌱 Backend Data Generator
The DataGenerator automatically creates:
- Users  
- Locations + sectors + seats  
- Events (with images)  
- Tickets & reservations  
- Artists (bands + members)  
- Merchandise  
- News items  

---

## 🧩 Tech Stack

### Frontend
- **Angular 17**
- TypeScript
- SCSS
- Custom Seatmap Rendering Component

### Backend
- **Spring Boot 3**
- Java 21
- Spring Security
- JPA / Hibernate
- REST API (JSON)

### Database
- H2 (dev mode)
- Full JPA model for events, seats, sectors, price categories, tickets, etc.

---

## 📁 Project Structure
