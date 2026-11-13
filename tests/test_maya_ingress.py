"""Phase-two manifests: run with python -m unittest discover -s tests -p test_maya_ingress.py."""
import unittest
from pathlib import Path
import yaml

ROOT = Path(__file__).resolve().parents[2]


class MayaIngressTest(unittest.TestCase):
    def test_all_public_maya_paths_use_gateway(self):
        paths = [ROOT / 'ty-multiverse-gateway/k8s/maya-ingress.yaml',
                 ROOT / 'peoplesystem-terraform-oke/tymultiverse-domain/mirror-ingress.yaml']
        docs = [doc for path in paths for doc in yaml.safe_load_all(path.read_text(encoding='utf-8')) if doc]
        selected = [doc for doc in docs if doc['metadata']['name'] in {
            'maya-sawa-ingress', 'portfolio-api-protected-ingress',
            'tymcc-maya-sawa', 'tymcc-portfolio-api-protected'}]
        self.assertEqual(len(selected), 4)
        for doc in selected:
            for rule in doc['spec']['rules']:
                for route in rule['http']['paths']:
                    self.assertEqual(route['backend']['service'], {
                        'name': 'ty-multiverse-gateway-service', 'port': {'number': 8082}})
            if 'portfolio' not in doc['metadata']['name']:
                annotations = doc['metadata']['annotations']
                self.assertEqual(annotations['nginx.ingress.kubernetes.io/proxy-body-size'], '1024m')
                self.assertEqual(annotations['nginx.ingress.kubernetes.io/proxy-request-buffering'], 'off')
                self.assertEqual(annotations['nginx.ingress.kubernetes.io/proxy-read-timeout'], '1800')

    def test_maya_manifest_does_not_publish_ingress(self):
        docs = yaml.safe_load_all((ROOT / 'maya-sawa/k8s/deployment.yaml').read_text(encoding='utf-8'))
        self.assertFalse(any(doc and doc.get('kind') == 'Ingress' for doc in docs))
