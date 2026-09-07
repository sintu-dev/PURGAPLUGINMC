@echo off  
"C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javac.exe" -cp "bin;lib/*" -d bin src/test/java/PurgaSystemTest.java  
"C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\java.exe" -cp "bin;lib/*" PurgaSystemTest 
