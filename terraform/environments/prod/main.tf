resource "kubernetes_namespace" "env" {
  metadata {
    name = var.namespace
  }
}

locals {
  common_env = {
    SPRING_PROFILES_ACTIVE       = var.env
    SPRING_CONFIG_IMPORT         = "optional:configserver:http://cloud-config-container:9296/"
    SPRING_ZIPKIN_BASE_URL       = "http://zipkin:9411"
    EUREKA_CLIENT_REGION         = "default"
    EUREKA_CLIENT_SERVICEURL_MYZONE = "http://service-discovery-container:8761/eureka"
    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://service-discovery-container:8761/eureka/"
  }
}

# ZIPKIN
module "zipkin" {
  source     = "../../modules/microservice"
  name       = "zipkin"
  image      = "openzipkin/zipkin"
  replicas   = 1
  ports      = [9411]
  namespace  = kubernetes_namespace.env.metadata[0].name

  env_vars = {}

  providers = {
    kubernetes = kubernetes
  }
}

# SERVICE DISCOVERY
module "service_discovery" {
  source     = "../../modules/microservice"
  name       = "service-discovery-container"
  image      = "saraluciaaa/service-discovery-ecommerce-boot:${var.docker_tag}"
  replicas   = var.replicas
  ports      = [8761]

  providers = {
    kubernetes = kubernetes
  }

  env_vars = {
    SPRING_PROFILES_ACTIVE = var.env
    SPRING_ZIPKIN_BASE_URL = "http://zipkin:9411"
    SPRING_CONFIG_IMPORT   = "optional:configserver:http://cloud-config-container:9296/"
  }

  namespace  = kubernetes_namespace.env.metadata[0].name
}

# CLOUD CONFIG
module "cloud_config" {
  source = "../../modules/microservice"
  name   = "cloud-config-container"
  image  = "saraluciaaa/cloud-config-ecommerce-boot:${var.docker_tag}"
  replicas = var.replicas
  ports  = [9296]
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name

  providers = {
    kubernetes = kubernetes
  }
}

# API GATEWAY
module "api_gateway" {
  source     = "../../modules/microservice"
  name       = "api-gateway-container"
  image      = "saraluciaaa/api-gateway-ecommerce-boot:${var.docker_tag}"
  replicas   = var.replicas
  ports      = [8080]
  service_type = "LoadBalancer"
  env_vars   = local.common_env
  namespace  = kubernetes_namespace.env.metadata[0].name
}

# MICROSERVICIOS
module "order_service" {
  source = "../../modules/microservice"
  name   = "order-service-container"
  image  = "saraluciaaa/order-service-ecommerce-boot:${var.docker_tag}"
  ports  = [8300]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "payment_service" {
  source = "../../modules/microservice"
  name   = "payment-service-container"
  image  = "saraluciaaa/payment-service-ecommerce-boot:${var.docker_tag}"
  ports  = [8400]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "product_service" {
  source = "../../modules/microservice"
  name   = "product-service-container"
  image  = "saraluciaaa/product-service-ecommerce-boot:${var.docker_tag}"
  ports  = [8500]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "shipping_service" {
  source = "../../modules/microservice"
  name   = "shipping-service-container"
  image  = "saraluciaaa/shipping-service-ecommerce-boot:${var.docker_tag}"
  ports  = [8600]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "user_service" {
  source = "../../modules/microservice"
  name   = "user-service-container"
  image  = "saraluciaaa/user-service-ecommerce-boot:${var.docker_tag}"
  ports  = [8700]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "favourite_service" {
  source = "../../modules/microservice"
  name   = "favourite-service-container"
  image  = "saraluciaaa/favourite-service-ecommerce-boot:${var.docker_tag}"
  ports  = [8800]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "proxy_client" {
  source = "../../modules/microservice"
  name   = "proxy-client-container"
  image  = "saraluciaaa/proxy-client-ecommerce-boot:${var.docker_tag}"
  ports  = [8900]
  replicas = var.replicas
  env_vars = local.common_env
  namespace = kubernetes_namespace.env.metadata[0].name
}

module "observability" {
  source           = "../../modules/observability"
  namespace        = "monitoring"
  grafana_password = var.grafana_password

  providers = {
    helm       = helm
    kubernetes = kubernetes
  }
}
