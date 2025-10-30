import os
import random
import string
from datetime import datetime

from locust import HttpUser, task, between, SequentialTaskSet, events


BASE_PATH = os.getenv("LOCUST_BASE_PATH", "").rstrip("/")
USERNAME = os.getenv("LOCUST_USERNAME", "test")
PASSWORD = os.getenv("LOCUST_PASSWORD", "password")


def _rand_username(prefix: str = "user") -> str:
    return f"{prefix}_{''.join(random.choices(string.ascii_lowercase + string.digits, k=6))}"


class EcommerceFlows(SequentialTaskSet):
    def on_start(self):
        self.jwt = None
        self._authenticate()

    # ---------- Helper calls ----------
    def _authenticate(self):
        payload = {"username": USERNAME, "password": PASSWORD}
        with self.client.post(f"{BASE_PATH}/api/authenticate", json=payload, name="POST /api/authenticate", catch_response=True) as resp:
            if resp.status_code == 200 and resp.json().get("jwtToken"):
                self.jwt = resp.json()["jwtToken"]
                resp.success()
            else:
                resp.failure(f"Auth failed: {resp.status_code} {resp.text}")

    def _auth_headers(self):
        return {"Authorization": f"Bearer {self.jwt}"} if self.jwt else {}

    # ---------- Flows ----------
    @task(3)
    def list_products(self):
        self.client.get(f"{BASE_PATH}/api/products", name="GET /api/products")

    @task(2)
    def create_order(self):
        order = {
            "orderDesc": "Locust order",
            "orderFee": round(random.uniform(10.0, 200.0), 2),
            "cart": {"userId": 1}
        }
        self.client.post(
            f"{BASE_PATH}/api/orders",
            headers=self._auth_headers(),
            json=order,
            name="POST /api/orders",
        )

    @task(2)
    def create_order_and_pay(self):
        order = {
            "orderDesc": "Order+Pay",
            "orderFee": round(random.uniform(5.0, 150.0), 2),
            "cart": {"userId": 1}
        }
        with self.client.post(
            f"{BASE_PATH}/api/orders",
            headers=self._auth_headers(),
            json=order,
            name="POST /api/orders (for payment)",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"Order failed: {resp.status_code}")
                return
            created = resp.json()
            payment = {
                "order": {
                    "orderId": created.get("orderId"),
                    "orderDesc": created.get("orderDesc"),
                    "orderFee": created.get("orderFee"),
                    "orderDate": created.get("orderDate"),
                }
            }
            self.client.post(
                f"{BASE_PATH}/api/payments",
                headers=self._auth_headers(),
                json=payment,
                name="POST /api/payments",
            )

    @task(1)
    def favourites_add_and_list(self):
        fav = {
            "userId": 1,
            "productId": 1,
            "likeDate": datetime.utcnow().strftime("%d-%m-%Y__%H:%M:%S:%f"),
        }
        self.client.post(
            f"{BASE_PATH}/api/favourites",
            headers=self._auth_headers(),
            json=fav,
            name="POST /api/favourites",
        )
        self.client.get(
            f"{BASE_PATH}/api/favourites",
            headers=self._auth_headers(),
            name="GET /api/favourites",
        )

    @task(1)
    def signup_then_login(self):
        username = _rand_username("locust")
        new_user = {
            "firstName": "Load",
            "lastName": "Tester",
            "email": f"{username}@example.com",
            "phone": "3000000000",
            "credential": {
                "username": username,
                "password": "pwd",
                "roleBasedAuthority": "ROLE_USER",
            },
        }
        self.client.post(
            f"{BASE_PATH}/api/users",
            headers=self._auth_headers(),
            json=new_user,
            name="POST /api/users",
        )
        # Login with new user
        payload = {"username": username, "password": "pwd"}
        self.client.post(
            f"{BASE_PATH}/api/authenticate",
            json=payload,
            name="POST /api/authenticate (new user)",
        )


class WebsiteUser(HttpUser):
    tasks = [EcommerceFlows]
    wait_time = between(float(os.getenv("LOCUST_WAIT_MIN", 0.2)), float(os.getenv("LOCUST_WAIT_MAX", 1.0)))