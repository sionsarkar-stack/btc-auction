# Native Ubuntu Deployment

This deployment serves the Vite build with Nginx and runs the Spring Boot backend with systemd. Nginx proxies `/api/` and `/ws/` to the backend, so the frontend can keep relative API URLs. These instructions target an Ubuntu 24.04 LTS EC2 instance; the backend requires Java 21.

## 1. Install packages

On Ubuntu 24.04 LTS or newer:

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless nginx
java -version
```

The build machine needs JDK 21; the EC2 server only needs JRE 21. Build the backend, then copy the jar to the server:

```bash
cd backend/btc-auction
./mvnw clean package -DskipTests
scp target/*.jar ubuntu@EC2_HOST:/tmp/btc-auction.jar
```

Build the frontend:

```bash
cd frontend/btc-auction-ui
npm ci
npm run build
scp -r dist ubuntu@EC2_HOST:/tmp/btc-auction-dist
```

## 2. Install the application

```bash
sudo useradd --system --home /opt/btc-auction --shell /usr/sbin/nologin btc-auction
sudo mkdir -p /opt/btc-auction/data /etc/btc-auction /var/www/btc-auction
sudo chown -R btc-auction:btc-auction /opt/btc-auction
sudo mv /tmp/btc-auction.jar /opt/btc-auction/btc-auction.jar
sudo cp -r /tmp/btc-auction-dist/* /var/www/btc-auction/
```

Create the environment file:

```bash
sudo cp deploy/systemd/btc-auction.env.example /etc/btc-auction/btc-auction.env
sudo nano /etc/btc-auction/btc-auction.env
sudo chmod 600 /etc/btc-auction/btc-auction.env
```

Install and start systemd:

```bash
sudo cp deploy/systemd/btc-auction.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now btc-auction
sudo systemctl status btc-auction
```

## 3. Configure Nginx

Replace `auction.example.com` in `deploy/nginx/btc-auction.conf` with the EC2 DNS name or domain, then run:

```bash
sudo cp deploy/nginx/btc-auction.conf /etc/nginx/sites-available/btc-auction
sudo ln -s /etc/nginx/sites-available/btc-auction /etc/nginx/sites-enabled/btc-auction
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

The `/ws/` location must keep HTTP/1.1 upgrade headers; otherwise live auction and silent-bid updates will not work.

## 4. HTTPS

Point the domain DNS record to the EC2 public address, then install a certificate:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d auction.example.com
```

## 5. Firewall

Configure the EC2 security group before connecting:

- Allow inbound TCP `22` only from your administration IP.
- Allow inbound TCP `80` and `443` from the public internet.
- Do not add an inbound rule for TCP `8080`.

Then allow the same traffic through the Ubuntu firewall:

```bash
sudo ufw allow from YOUR_IP to any port 22 proto tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Do not expose port `8080` in the EC2 security group. The application database is stored under `/opt/btc-auction/data`; back up that directory regularly, or move to PostgreSQL/RDS before scaling beyond one instance.

## Operations

```bash
sudo journalctl -u btc-auction -f
sudo systemctl restart btc-auction
sudo nginx -t && sudo systemctl reload nginx
```
