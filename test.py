import urllib.request
import json
import sys

def parse_yaml():
    try:
        import yaml
        with open("user-service/src/main/resources/application-prod.yml") as f:
            data = yaml.safe_load(f)
            print(json.dumps(data, indent=2))
    except ImportError:
        import sys
        print("No pyyaml")
        sys.exit(1)

parse_yaml()
