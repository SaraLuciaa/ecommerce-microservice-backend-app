data "digitalocean_kubernetes_versions" "current" {
  version_prefix = "1.32."
}

resource "digitalocean_kubernetes_cluster" "dev" {
  name    = var.cluster_name
  region  = var.region
  version = data.digitalocean_kubernetes_versions.current.latest_version

  node_pool {
    name       = var.node_pool_name
    size       = var.node_size
    node_count = var.node_count
  }

  lifecycle {
    ignore_changes = [
      node_pool[0].tags,
      node_pool[0].labels,
      tags,
    ]
  }
}
