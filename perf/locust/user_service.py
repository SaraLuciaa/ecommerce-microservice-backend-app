"""
User Service Performance Tests for Minikube
"""
import random
import string
from locust import HttpUser, task, between

BASE_PATH = "/user-service"


def random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))


def random_email():
    return f"user_{random_string()}@test.com"


def random_phone():
    return f"+1{random.randint(1000000000, 9999999999)}"


class UserServiceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Initialize test data"""
        self.created_user_ids = []
        self.created_address_ids = []
    
    # ==================== User Endpoints ====================
    
    @task(10)
    def get_all_users(self):
        """GET /api/users - List all users"""
        self.client.get(
            f"{BASE_PATH}/api/users",
            name="GET /api/users"
        )
    
    @task(5)
    def get_user_by_id(self):
        """GET /api/users/{userId} - Get user by ID"""
        user_id = random.choice(self.created_user_ids) if self.created_user_ids else random.randint(1, 10)
        self.client.get(
            f"{BASE_PATH}/api/users/{user_id}",
            name="GET /api/users/{userId}",
            catch_response=True
        )
        
    @task(3)
    def create_user(self):
        """POST /api/users - Create user with credentials and address"""
        username = f"user_{random_string(8)}"
        user_data = {
            "firstName": f"First_{random_string(5)}",
            "lastName": f"Last_{random_string(5)}",
            "email": random_email(),
            "phone": random_phone(),
            "imageUrl": f"https://example.com/{random_string()}.jpg",
            "credential": {
                "username": username,
                "password": f"P@ss{random_string(6)}123",
                "roleBasedAuthority": "ROLE_USER"
            },
            "addressDtos": [
                {
                    "fullAddress": f"{random.randint(1, 9999)} {random.choice(['Main', 'Oak', 'Pine', 'Elm'])} St",
                    "city": random.choice(["Cali", "Bogota", "Medellin", "Barranquilla", "Cartagena"]),
                    "postalCode": f"{random.randint(100000, 999999)}"
                }
            ]
        }
        
        with self.client.post(
            f"{BASE_PATH}/api/users",
            json=user_data,
            name="POST /api/users",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                try:
                    user = resp.json()
                    if user.get("userId"):
                        self.created_user_ids.append(user["userId"])
                    resp.success()
                except:
                    resp.success()
            else:
                resp.failure(f"Failed: {resp.status_code}")
    
    # ==================== Address Endpoints ====================
    
    @task(5)
    def get_all_addresses(self):
        """GET /api/address"""
        self.client.get(
            f"{BASE_PATH}/api/address",
            name="GET /api/address"
        )
    
    @task(3)
    def get_address_by_id(self):
        """GET /api/address/{addressId}"""
        address_id = random.choice(self.created_address_ids) if self.created_address_ids else random.randint(1, 10)
        self.client.get(
            f"{BASE_PATH}/api/address/{address_id}",
            name="GET /api/address/{addressId}",
            catch_response=True
        )
    
    # ==================== Credential Endpoints ====================
    
    @task(5)
    def get_all_credentials(self):
        """GET /api/credentials"""
        self.client.get(
            f"{BASE_PATH}/api/credentials",
            name="GET /api/credentials"
        )
    
    @task(3)
    def get_credential_by_id(self):
        """GET /api/credentials/{credentialId}"""
        credential_id = random.randint(1, 10)
        self.client.get(
            f"{BASE_PATH}/api/credentials/{credential_id}",
            name="GET /api/credentials/{credentialId}"
        )