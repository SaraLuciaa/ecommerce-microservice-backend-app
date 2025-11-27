"""
Product Service Performance Tests for Minikube
"""
import random
import string
from locust import HttpUser, task, between

BASE_PATH = "/product-service"


def random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))


class ProductServiceUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        """Initialize test data"""
        self.created_product_ids = []
        self.created_category_ids = []
    
    # ==================== Product Endpoints ====================
    
    @task(10)
    def get_all_products(self):
        """GET /api/products - List all products"""
        self.client.get(
            f"{BASE_PATH}/api/products",
            name="GET /api/products"
        )
    
    @task(5)
    def get_product_by_id(self):
        """GET /api/products/{productId}"""
        product_id = random.randint(1, 4)
        self.client.get(
            f"{BASE_PATH}/api/products/{product_id}",
            name="GET /api/products/{productId}",
            catch_response=True
        )
    
    @task(3)
    def create_product(self):
        """POST /api/products - Create product"""
        product_data = {
            "productTitle": f"Product_{random_string(6)}",
            "imageUrl": f"https://example.com/products/{random_string()}.jpg",
            "sku": f"SKU-{random_string(10).upper()}",
            "priceUnit": round(random.uniform(10.0, 500.0), 2),
            "quantity": random.randint(1, 100),
            "category": {
                "categoryId": random.randint(1, 3)
            }
        }
        
        with self.client.post(
            f"{BASE_PATH}/api/products",
            json=product_data,
            name="POST /api/products",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                try:
                    product = resp.json()
                    if product.get("productId"):
                        self.created_product_ids.append(product["productId"])
                    resp.success()
                except:
                    resp.success()
            else:
                resp.failure(f"Failed: {resp.status_code}")
    
    @task(2)
    def update_product(self):
        """PUT /api/products/{productId}"""
        product_id = random.randint(1, 4)
        product_data = {
            "productId": product_id,
            "productTitle": f"Updated_{random_string(6)}",
            "imageUrl": f"https://example.com/products/{random_string()}.jpg",
            "sku": f"SKU-{random_string(10).upper()}",
            "priceUnit": round(random.uniform(10.0, 500.0), 2),
            "quantity": random.randint(1, 100),
            "category": {
                "categoryId": random.randint(1, 3)
            }
        }
        
        with self.client.put(
            f"{BASE_PATH}/api/products/{product_id}",
            json=product_data,
            name="PUT /api/products/{productId}",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Unexpected: {resp.status_code}")
    
    # ==================== Category Endpoints ====================
    
    @task(5)
    def get_all_categories(self):
        """GET /api/categories - List all categories"""
        self.client.get(
            f"{BASE_PATH}/api/categories",
            name="GET /api/categories"
        )
    
    @task(3)
    def get_category_by_id(self):
        """GET /api/categories/{categoryId}"""
        category_id = random.randint(1, 3) 
        with self.client.get(
            f"{BASE_PATH}/api/categories/{category_id}",
            name="GET /api/categories/{categoryId}",
            catch_response=True
        ) as resp:
            if resp.status_code in [200]:
                resp.success()
            else:
                resp.failure(f"Unexpected: {resp.status_code}") 
    
    @task(2)
    def create_category(self):
        """POST /api/categories - Create category with parentCategory"""
        category_data = {
            "categoryTitle": f"Category_{random_string(6)}",
            "imageUrl": f"https://example.com/categories/{random_string()}.jpg",
            "parentCategory": {
                "categoryId": random.randint(1, 3)
            }
        }
        
        with self.client.post(
            f"{BASE_PATH}/api/categories",
            json=category_data,
            name="POST /api/categories",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                try:
                    category = resp.json()
                    if category.get("categoryId"):
                        self.created_category_ids.append(category["categoryId"])
                    resp.success()
                except:
                    resp.success()
            else:
                resp.failure(f"Failed: {resp.status_code}")
    
    @task(1)
    def update_category(self):
        """PUT /api/categories/{categoryId}"""
        category_id = random.randint(1, 3)
        category_data = {
            "categoryId": category_id,
            "categoryTitle": f"Updated_{random_string(6)}",
            "imageUrl": f"https://example.com/categories/{random_string()}.jpg",
            "parentCategory": {
                "categoryId": random.randint(1, 3)
            }
        }
        
        with self.client.put(
            f"{BASE_PATH}/api/categories/{category_id}",
            json=category_data,
            name="PUT /api/categories/{categoryId}",
            catch_response=True
        ) as resp:
            if resp.status_code in [200]:
                resp.success()
            else:
                resp.failure(f"Unexpected: {resp.status_code}")
