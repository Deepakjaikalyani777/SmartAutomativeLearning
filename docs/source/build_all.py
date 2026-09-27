"""Regenerate every PDF in /docs.  Usage: cd docs/source && python3 build_all.py"""
import doc00_start_here, doc01_project_structure, doc02_concepts, doc03_security, doc04_interview
from hf import build

build("../00_Start_Here.pdf", "00 Start Here - Automotive Academy", "Setup and learning plan", doc00_start_here.story())
build("../01_Project_Structure.pdf", "01 Project Structure - Automotive Academy", "Codebase tour", doc01_project_structure.story())
build("../02_Automotive_Concepts.pdf", "02 Automotive Concepts - Automotive Academy", "AAOS concepts", doc02_concepts.story())
build("../03_Automotive_Security.pdf", "03 Automotive Security - Automotive Academy", "Security", doc03_security.story())
build("../04_Interview_QA.pdf", "04 Interview Q&A - Automotive Academy", "Interview preparation", doc04_interview.story())
print("done")
