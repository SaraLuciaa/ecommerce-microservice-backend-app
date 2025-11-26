resource "kubernetes_deployment" "this" {
  metadata {
    name = var.name
    labels = {
      app = var.name
    }
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = {
        app = var.name
      }
    }

    template {
      metadata {
        labels = {
          app = var.name
        }
      }

      spec {
        container {
          image = var.image
          name  = var.name

          dynamic "port" {
            for_each = var.ports
            content {
              container_port = port.value
            }
          }

          dynamic "env" {
            for_each = var.env_vars
            content {
              name  = env.key
              value = env.value
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "this" {
  metadata {
    name = var.name
  }
  spec {
    selector = {
      app = var.name
    }
    dynamic "port" {
      for_each = var.ports
      content {
        port        = port.value
        target_port = port.value
      }
    }
    type = var.service_type
  }
}
