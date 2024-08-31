To deploy the app itself:
```
mvn -Pdocker package
docker save -o project-bootstrap.tar project-bootstrap:$(git rev-parse HEAD)
ansible-playbook -i hosts.ini deploy.yml --extra-vars "image_name=project-bootstrap:$(git rev-parse HEAD)"
```
