The configuration is as follows:
- 1 load balancer node
- 4 aerospike nodes
- 5 http server nodes

Aerospike setup:
```
cd aerospike
ansible-playbook --extra-vars "ansible_user=<user> ansible_password=<password> ansible_ssh_extra_args='-o StrictHostKeyChecking=no'" -i hosts aerospike.yaml
```

To deploy the app itself:
```
mvn -Pdocker package
docker save -o project-bootstrap.tar project-bootstrap:$(git rev-parse HEAD)
ansible-playbook -i hosts.ini deploy.yml --extra-vars "image_name=project-bootstrap:$(git rev-parse HEAD)"
```

To launch the load balancer:
```
cd load_balancer
sudo docker build -t my-proxy .
sudo docker run --network=host --privileged my-proxy
```
