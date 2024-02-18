import socket
import threading

def handle_client(client_socket, addr):
    try:
        while True:
            data = client_socket.recv(1024)
            if not data:
                break  # Connection closed by the client
            print(f"Received from {addr}: {data.decode()}")
            client_socket.sendall(data)  # Echo back the received message
    except Exception as e:
        print(f"Error with {addr}: {e}")
    finally:
        client_socket.close()
        print(f"Connection to {addr} closed")

def start_server(port):
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('', port))
    server_socket.listen()

    print(f'Server started, listening on port {port}...')

    try:
        while True:
            client_socket, addr = server_socket.accept()
            print(f'Connection from {addr} established')
            # Handle client in a new thread
            client_thread = threading.Thread(target=handle_client, args=(client_socket, addr))
            client_thread.start()
    except KeyboardInterrupt:
        print("Server is shutting down...")
    finally:
        server_socket.close()
        print("Server closed")

if __name__ == '__main__':
    start_server(3001)  # Specify your port number here