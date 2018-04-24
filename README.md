# minijava

## How do I run only the tests of a particular package from IntelliJ?
Right-click the package/class you want to run and click on "Run 'Tests in package.name'"

## How do I run only the tests of a particular package from the terminal?
- Go to the root module folder (ir, backend, or frontend)
- Run `mvn -DfailIfNoTests=false "-Dtest=test/parser/*.java" test` (replace test/parser for whichever the package/name is)
