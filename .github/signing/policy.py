from verify_artifact import Policy
from signer import main
POLICY=Policy(repository='Oxiom-Systems/watch-my-ai-doing',repository_id=1317333090,workflow_path='.github/workflows/manual-build.yml',recipe_ref='Oxiom-Systems/watch-my-ai-doing/.github/workflows/ci-build-recipe.yml@7e3f7b10a2d997fa1089551b62e243841008db20',producer_job='build / build',group_id=5,build_label='ci-linux-cpu-v1-r1317333090',artifact_kind='apk',artifact_stem='wmaid-unsigned')
if __name__=='__main__':raise SystemExit(main(POLICY))
