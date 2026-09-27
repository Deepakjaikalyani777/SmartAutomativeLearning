from hf import *
import doc02a
import doc02b


def story():
    s = cover("Automotive Concepts", "Android Automotive OS and the car around it",
              "\"If you can explain it with a real car in front of you, you understand it.\"",
              ["17 chapters, each tied to an animated lesson", "real cars, real standards, real APIs",
               "exercises with answers at the end"])
    s += toc()
    s += doc02a.chapters()
    s += doc02b.chapters()
    return s


if __name__ == "__main__":
    build("../02_Automotive_Concepts.pdf", "02 Automotive Concepts - Automotive Academy", "AAOS concepts", story())
