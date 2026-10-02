<div align="center">

# ⚖️ Load Balancer Simulator

**A desktop application for simulating and comparing 13 load-balancing algorithms, built with Java and Swing.**

![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/GUI-Swing-blue)
![Algorithms](https://img.shields.io/badge/Algorithms-13-orange)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)

[About](#-about) · [Features](#-features) · [Algorithms](#-supported-algorithms) · [Getting Started](#-getting-started) · [Project Structure](#-project-structure) · [Contributing](#-contributing)

<!-- 📸 Add a screenshot or GIF of the application here, e.g.: -->
<!-- <img src="docs/screenshot.png" alt="Load Balancer Simulator" width="80%"> -->

</div>

---

## 💡 About

In distributed systems, a **load balancer** spreads incoming requests across multiple servers to improve performance, availability, and resource utilization. The choice of algorithm has a major impact on how a system behaves under load, from simple rotation to health-aware, latency-aware, and adaptive strategies.

**Load Balancer Simulator** is a desktop tool that lets you experiment with a wide range of load-balancing strategies in a safe, visual environment, with no real servers required. It is useful for **students**, **educators**, and **engineers** who want to understand how these algorithms work and how they compare.

## ✨ Features

- 🖥️ **Desktop GUI** built with Java Swing, with no browser or server needed
- 🧮 **13 load-balancing algorithms**, from classic to production-grade strategies
- 📊 **Visual feedback** on how requests are distributed across servers
- 🔬 **Side-by-side experimentation**: switch algorithms and compare behavior
- 🧩 **Extensible design**, so new algorithms are easy to add
- 🪶 **Lightweight**: pure Java, no external dependencies

## 🧮 Supported Algorithms

### Basic

| Algorithm | Description |
|-----------|-------------|
| **Round Robin** | Distributes requests to servers in a fixed, repeating order. |
| **Weighted Round Robin** | Round Robin where servers with higher weights receive proportionally more requests. |

### Load-Aware

| Algorithm | Description |
|-----------|-------------|
| **Least Connections** | Sends each request to the server with the fewest active connections. |
| **Weighted Least Connections** | Least Connections that also accounts for each server's capacity (weight). |
| **Power of Two Choices** | Samples two servers at random and picks the less loaded one, giving near-optimal balance with very low overhead. |
| **Join-Idle-Queue (JIQ)** | Routes requests to servers that have reported themselves idle, reducing the dispatcher's load-query overhead. |

### Performance-Aware & Adaptive

| Algorithm | Description |
|-----------|-------------|
| **Latency Based** | Prefers the server with the lowest observed response time. |
| **Resource Aware** | Chooses servers based on resource utilization such as CPU and memory. |
| **Adaptive Feedback** | Continuously adjusts routing decisions using feedback from observed server performance. |

### Affinity & Hashing

| Algorithm | Description |
|-----------|-------------|
| **Consistent Hashing** | Maps requests and servers onto a hash ring so that adding or removing a server remaps only a small fraction of keys. |
| **Sticky Sessions** | Keeps routing the same client to the same server (session affinity). |

### Resilience & Infrastructure

| Algorithm | Description |
|-----------|-------------|
| **Health Check Dynamic Pool** | Monitors server health and dynamically removes or restores servers in the active pool. |
| **Service Mesh Sidecar** | Models sidecar-proxy-based balancing, as used in service-mesh architectures. |

## 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java |
| GUI Toolkit | Swing (`javax.swing`) |
| Build | Plain `javac` (works with Eclipse, IntelliJ IDEA, NetBeans, or the command line) |

## ⚙️ Getting Started

### Prerequisites

- **JDK 8 or newer**: check with `java -version`
- **Git**

### Installation & Run

```bash
# 1. Clone the repository
git clone https://github.com/basharmansour-it-eng/Load-Balancer-Simulator.git
cd Load-Balancer-Simulator

# 2. Compile the source files into the bin folder
#    Linux / macOS
javac -d bin $(find src -name "*.java")
#    Windows (PowerShell)
#    javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName

# 3. Run the application (replace MainClass with your entry point)
java -cp bin MainClass
```

### Run from an IDE

1. Open **Eclipse / IntelliJ IDEA / NetBeans**.
2. Import the project folder (`File → Open Projects from File System…`).
3. Locate the class containing the `main` method and click **Run**.

## 🚀 Usage

1. Launch the application.
2. Select one of the **13 load-balancing algorithms**.
3. Configure the simulation (servers, incoming requests, etc.).
4. Start the simulation and observe how requests are distributed.
5. Switch to another algorithm and compare the results.

## 📁 Project Structure

```text
Load-Balancer-Simulator/
├── src/            # Java source code (algorithms and GUI)
├── bin/            # Compiled classes
├── .gitignore
└── README.md
```

## 🗺 Roadmap

- [x] Desktop GUI with Swing
- [x] 13 load-balancing algorithms
- [ ] Charts and performance metrics (response time, throughput, server utilization)
- [ ] Export simulation results to CSV
- [ ] Side-by-side algorithm comparison view
- [ ] Unit tests for each algorithm
- [ ] Packaged runnable `.jar` release

## 🤝 Contributing

Contributions, ideas, and bug reports are welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push the branch: `git push origin feature/my-feature`
5. Open a Pull Request

## 📄 License

This project does not have a license yet. To allow others to use and contribute to it, consider adding one, for example the [MIT License](https://choosealicense.com/licenses/mit/).

## 👤 Author

**Bashar Mansour**
GitHub: [@basharmansour-it-eng](https://github.com/basharmansour-it-eng)

---

<div align="center">

⭐ If you find this project useful, please consider giving it a star!

</div>
