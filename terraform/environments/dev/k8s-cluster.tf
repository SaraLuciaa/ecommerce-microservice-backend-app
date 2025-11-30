resource "digitalocean_kubernetes_cluster" "dev" {
  name    = var.cluster_name
  region  = var.region
  version = var.k8s_version

  node_pool {
    name       = "${var.cluster_name}-pool"
    size       = var.node_size
    node_count = var.node_count
    
    tags = ["dev", "ecommerce", "microservices"]
    
    labels = {
      environment = "dev"
      service     = "ecommerce"
    }
  }

  tags = ["dev", "ecommerce-backend"]
}
