# LeafMe

LeafMe is an Android application for monitoring and managing your plants. It allows users to track plant moisture levels, temperature, and set watering schedules, helping plant enthusiasts keep their green friends healthy and thriving.

## Features

- User authentication (registration and login)
- Plant management (add, remove, and monitor plants)
- Real-time plant moisture and temperature monitoring
- Watering schedule and notifications
- Synchronization with server for seamless data access across devices
- Optional manual plant ID assignment for specific plant monitoring devices

## Technologies

- Kotlin
- Jetpack Compose for modern UI
- MVVM Architecture
- Room Database for local storage
- Retrofit for network operations
- Coroutines and Flow for asynchronous operations
- Material 3 Design

## Getting Started

1. Clone the repository
2. Open the project in Android Studio
3. Update the server URL in `RetrofitClient.kt` and in network-security-config file in res to point to your backend
4. Build and run the application

## Backend Server

This application works with a backend server that handles plant data and authentication. You can find the server code at:
- [Plant IoT Server](https://github.com/B3r3z/plant_IoT_server)

## Architecture

LeafMe follows the MVVM (Model-View-ViewModel) architecture pattern with a clean separation of concerns:

- **Model**: Data classes and repositories for data access
- **View**: Compose UI components
- **ViewModel**: Business logic and state management

The application handles both online and offline scenarios, with the server as the source of truth for data synchronization.

## Authors

- Bartosz Berezowski
- Piotr Truszkowski

## License

This project is licensed under the MIT License - see below for details:

```
MIT License

Copyright (c) 2025 LeafMe Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Acknowledgments

- [Retrofit](https://github.com/square/retrofit) for API communication
- [Room](https://developer.android.com/jetpack/androidx/releases/room) for database operations
- [Material Design](https://material.io/design) for UI guidelines
