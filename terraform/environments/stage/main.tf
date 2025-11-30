resource "kubernetes_namespace" "stage" {
  metadata {
    name = "stage"
  }
}

module "zipkin" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "zipkin"
  image    = "openzipkin/zipkin"
  replicas = 1
  ports    = [9411]
  namespace = "stage"
  depends_on = [kubernetes_namespace.stage]
}

module "service_discovery" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "service-discovery-container"
  image    = "saraluciaaa/service-discovery-ecommerce-boot:1.0.0stage"
  replicas = 2
  ports    = [8761]
  env_vars = {
    "SPRING_PROFILES_ACTIVE"     = "stage"
    "SPRING_ZIPKIN_BASE-URL"     = "http://zipkin:9411"
    "SPRING_CONFIG_IMPORT"       = "optional:configserver:http://cloud-config-container:9296/"
  }
  depends_on = [module.zipkin]
  namespace = "stage"
}

module "cloud_config" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "cloud-config-container"
  image    = "saraluciaaa/cloud-config-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [9296]
  env_vars = {
    "SPRING_PROFILES_ACTIVE"     = "stage"
    "SPRING_ZIPKIN_BASE-URL"     = "http://zipkin:9411"
    "EUREKA_CLIENT_REGION"       = "default"
    "EUREKA_CLIENT_AVAILABILITYZONES_DEFAULT" = "myzone"
    "EUREKA_CLIENT_SERVICEURL_MYZONE"      = "http://service-discovery-container:8761/eureka"
    "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE" = "http://service-discovery-container:8761/eureka/"
  }
  depends_on = [module.service_discovery]
  namespace = "stage"
}

locals {
  common_env = {
    "SPRING_PROFILES_ACTIVE"     = "stage"
    "SPRING_ZIPKIN_BASE-URL"     = "http://zipkin:9411"
    "SPRING_CONFIG_IMPORT"       = "optional:configserver:http://cloud-config-container:9296/"
    "EUREKA_CLIENT_REGION"       = "default"
    "EUREKA_CLIENT_AVAILABILITYZONES_DEFAULT" = "myzone"
    "EUREKA_CLIENT_SERVICEURL_MYZONE"      = "http://service-discovery-container:8761/eureka"
    "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE" = "http://service-discovery-container:8761/eureka/"
  }
}

module "api_gateway" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "api-gateway-container"
  image    = "saraluciaaa/api-gateway-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8080]
  service_type = "LoadBalancer"
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "order_service" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "order-service-container"
  image    = "saraluciaaa/order-service-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8300]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "payment_service" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "payment-service-container"
  image    = "saraluciaaa/payment-service-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8400]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "product_service" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "product-service-container"
  image    = "saraluciaaa/product-service-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8500]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "shipping_service" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "shipping-service-container"
  image    = "saraluciaaa/shipping-service-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8600]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "user_service" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "user-service-container"
  image    = "saraluciaaa/user-service-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8700]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "favourite_service" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "favourite-service-container"
  image    = "saraluciaaa/favourite-service-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8800]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}

module "proxy_client" {
  source = "git::https://github.com/SaraLuciaa/ecommerce-microservice-backend-app.git//terraform/modules/microservice?ref=stage"
  name     = "proxy-client-container"
  image    = "saraluciaaa/proxy-client-ecommerce-boot:1.0.0stage"
  replicas = 1
  ports    = [8900]
  env_vars = local.common_env
  depends_on = [module.cloud_config]
  namespace = "stage"
}