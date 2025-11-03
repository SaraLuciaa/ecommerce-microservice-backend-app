"""
Shipping Service Performance Tests for Minikube
"""
import random
from locust import HttpUser, task, between

BASE_PATH = "/shipping-service"


class ShippingServiceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Initialize test data"""
        self.created_items = []
    
    # ==================== OrderItem Endpoints ====================

    
    @task(3)
    def create_order_item(self):
        """POST /api/shippings - Create order item"""
        order_item_data = {
            "orderId": random.randint(1, 100),
            "productId": random.randint(1, 50),
            "orderedQuantity": random.randint(1, 10)
        }
        
        with self.client.post(
            f"{BASE_PATH}/api/shippings",
            json=order_item_data,
            name="POST /api/shippings",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                try:
                    item = resp.json()
                    if item.get("orderId") and item.get("productId"):
                        self.created_items.append({
                            "orderId": item["orderId"],
                            "productId": item["productId"]
                        })
                    resp.success()
                except:
                    resp.success()
            else:
                resp.failure(f"Failed: {resp.status_code}")
    
    @task(2)
    def update_order_item(self):
        """PUT /api/shippings"""
        if self.created_items:
            item = random.choice(self.created_items)
            order_id = item["orderId"]
            product_id = item["productId"]
        else:
            order_id = random.randint(1, 2)
            product_id = random.randint(1, 2)
        
        order_item_data = {
            "orderId": order_id,
            "productId": product_id,
            "orderedQuantity": random.randint(1, 20)
        }
        
        with self.client.put(
            f"{BASE_PATH}/api/shippings",
            json=order_item_data,
            name="PUT /api/shippings",
            catch_response=True
        ) as resp:
            if resp.status_code in [200]:
                resp.success()
            else:
                resp.failure(f"Unexpected: {resp.status_code}") 
