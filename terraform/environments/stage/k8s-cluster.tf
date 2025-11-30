resource "digitalocean_kubernetes_cluster" "stage" {
  name    = var.cluster_name
  region  = var.region
  version = var.k8s_version

  node_pool {
    name       = "${var.cluster_name}-pool"
    size       = var.node_size
    node_count = var.node_count
    
    tags = ["stage", "ecommerce", "microservices"]
    
    labels = {
      environment = "stage"
      service     = "ecommerce"
    }
  }

  tags = ["stage", "ecommerce-backend"]
}
