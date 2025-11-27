resource "kubernetes_deployment" "mysql" {
  metadata {
    name = var.name
    labels = {
      app = var.name
    }
  }

  spec {
    replicas = 1

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
          image = "mysql:5.7"
          name  = var.name

          port {
            container_port = 3306
          }

          env {
            name  = "MYSQL_ROOT_PASSWORD"
            value = var.root_password
          }

          env {
            name  = "MYSQL_DATABASE"
            value = var.database_name
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "mysql" {
  metadata {
    name = var.name
  }
  spec {
    selector = {
      app = var.name
    }
    port {
      port        = 3306
      target_port = 3306
    }
    type = "ClusterIP"
  }
}
