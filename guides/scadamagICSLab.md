# Justifying an ICS Lab

[Source](http://scadamag.infracritical.com/index.php/2018/12/03/justifying-an-ics-lab/)

December 3, 2018 -- Jake Brodsky

## Introduction

It dawned on me recently that that there is very little discussion on why an Industrial Control Systems (ICS) lab is needed or what it is used for. I am jotting down these notions in the hope that others can add to this discussion and help justify a lab to managers who may not understand what all this fuss is about.

If you’re from the ICS security camp, pay attention here. To correctly diagnose a security problem, you must first know how the control system was supposed to work and how it was built. This is not just about some computer network or server OS, this is the whole enchilada of process, instrumentation, controllers, networks, the screens, and the historian. If you want to diagnose a situation, you need to understand it from the very foundation of where it starts. To those of you approaching the field from the IT security side of the fence, please pay attention here. This is how you can safely learn about all the tips and tricks that Engineers and senior field technicians know.

So, why have a lab at all? It’s not cheap to set one up or keep it running.

- Have a place to examine failed parts to determine causes
- Mock up and test new systems BEFORE installing it in the field for the first time
- Practice what was taught in training
- Penetration testing of offline equipment
- Patch testing for field equipment
- Demonstrate functionality of new equipment under consideration

These are reasons enough for most Operations staff and every ICS consultant firm to have a lab. Not many mangers understand the depth and complexity of the instruments, the managed switches, the servers, the HMI workstations, the power systems, the networks, the wiring, the surge protection systems, the software, the PLC gear, the I/O panels and so forth.

It’s all about the care and feeding of engineers. When you say the word “empowerment” this is how you empower an engineer: By giving them the tools and context to confirm what they’re designing.

## Background

As an aside, control systems integrators are usually the last people working on a job before the keys are handed over to the end users. There are usually delays at that point, many change orders, schedule slips, and so forth. The General Contractor just wants the job to be over so that he can get paid. Any delay on behalf of the instrumentation or control system vendor is not usually met with much grace.

If you’re an end-user, this is one very good reason for you to ramp up the capabilities of your shop. It is all too commonplace for the project manager to turn over a very half baked instrumentation and control system that, while it may meet specification, isn’t completely functional. And no, by then, the change order authority is usually delegated to someone so high up in the company that you’ll likely never get anyone to change the project scope to deliver something more functional.

I’m not knocking how project management works, it’s just the reality that most organizations have learned to deal with. The project managers are usually exhausted toward the end of the project and the last thing they want to hear is that some automated thingy isn’t working the way it was expected to. Most notions of turn-key operation are nearly moot at that point. If the automation matters to you, be ready to pick things up and change it to your liking.

## Basics

So how much space do you need for a lab? Well, if you’re a small firm, you could build up a nice lab in the space it takes for a two car garage. A couple work benches, a whiteboard, a panel of wall with plywood, DIN rail and so forth would be nice. A couple of 19″ racks, one with conventional IT server rack size and one perhaps as a relay rack. Obviously if you routinely assemble panels or have larger equipment on a skid such as a Variable Frequency Drive (VFD) and a motor, or you’re testing instruments, you’ll need more space and probably more power. You should also have decent Internet access, a good firewall, and some auxiliary computing resources including good laptops with enough computing resources to run at least one virtualized image along side the existing operating system.

You’ll probably need space and ventilation for batteries on charge. Power systems for 12, 24, and 48 volt DC batteries on charge are often necessary for system mock-ups.

## Expendables

There are expendable items in the lab that you’ll be replenishing. These include DIN rail, a large assortment of fasteners (self tapping screws, machine screws, zip ties etc.), machine tool wire of various colors and gauges, and of course a wide assortment of fuses from cartridge fuses and fuse holders to small 1AG fuses and everything in between. You’ll need outlet strips, power cords, extension cords, wire loom, wire ducts, crimp tool wire lugs, RJ-11 and RJ 45 plugs and sockets, punch blocks, telephone wire, coaxial cables, twinaxial cables, junction boxes for electrical work, grounding rods and ground wire, wire nuts of various sizes, DIN terminal strips, DIN terminal bus-bar connectors, fuse connectors, precision 250 Ohm resistors (at least 1/2W and 1% or better accuracy), some Kindorf and various various connectors and brackets, an assortment of electrical tape in different colors, and so on.

My best guess is that you’ll spend about $50k just stocking up with all the expendables. Figure on spending at least that much for the first few years while you build up your stock in the shop. It helps to set up business accounts with distributors such as Allied, Branch Electric, Newark, Grainger, Graybar and so forth.

You’ll need hand tools too. Don’t go cheap. You might need a hand drill/screw gun, a punch tool for telephony (yes, lots of things are still done with telephony tools), an assortment of head lamps, flashlights, and drop-lights (a droplight rated for potentially explosive gas environment would be a very good idea), measuring tapes, a micrometer, a soldering and desoldering workstation, and so forth.

Another thing: get one of those pocket size voltage sensor devices to tell you if a panel is hot. USE IT! Never assume the panel you’re working on is safe. Make it safe, lock it or tag it, and check your own work. True, the panel may be marked as a low DC voltage panel, but that doesn’t mean that it actually is.

Finally, if you’re doing any installation work, a decent laser level guide, a good reciprocating saw, a hammer drill and other heavy tools will save you loads of time instead of waiting for an electrician to do that work for you. Some job sites have union work rules that may make trouble for you to use such tools on a job site, so check before doing anything. That said, you owe it to yourself to get familiar with those tools and make sure your work is neat. If you leave a site with cheap, sloppy, or shoddy work; that reputation will follow you for a long time to come. People on industrial sites have memories of elephants. I’ve seen them harass others over things that were done fifteen years ago. If you’re not comfortable doing installation work, pair up with a reputable industrial electrician firm. Any delay or extra cost will be more than worth the hit to your reputation from leaving cheap work behind.

## Test Equipment

There is also the need for test equipment. This is where things get expensive. You’ll need a few durable, accurate digital volt meters. I like the ProcessMeter from Fluke. With one of those you can simulate a 4-20 mA signal very easily. This invaluable, particularly when smoking out problems with a multi-leg 4-20 mA circuit where one side may be grounded. If you’re doing certification testing, or diagnostics, you’ll need a clamp-on ammeter. You can even get those clamp-on current meters for 4-20 mA current loops work. These meters are not cheap, but they’ll save you lots of time and hassle on a job site.

If you’re doing any networking with CAT5, CAT6 or similar cable, get a cable tester. Learn to use it correctly. You’re not just checking for continuity, you’re supposed to look at crosstalk between pairs, impedance/bandwidth characteristics, cable length, and so forth. People often make mistakes and try to splice the cable and hide the splice somewhere. Often, nobody notices, but sometimes it’s a serious problem. Know how to find those problems.

You should also get a decent optical laser and power meter, and a fiber optic TDR set –and learn to use it properly. By that, I mean learn all the background information about Time Domain Reflectometry and be able to explain every feature on that graph. You should be able to predict how much loss a run of fiber will have. Knowing that, you should then be able to account for every decibel of loss in a fiber run and to ensure that the cable is good for the reliable traffic the client requires. Go/No-go test sets will often pass cables that shouldn’t be accepted. Get it right or go home.

If you’re doing this sort of work a tool kit of connector installation equipment, a connector microscope, fusion splice equipment, and index gel (for temporary field splices) are essentials. All this sounds expensive, and historically it has been pretty pricey. However as more and more fiber infrastructure is installed, the prices have been dropping quickly. Still, don’t expect to get away from this without spending at least $20k.

Unfortunately my experience dealing with contractors who supposedly had certification and expertise in fiber-optic cabling has been poor. Maybe you know someone who can do this for you, but tread with care. There are lots of pretenders out there who are frighteningly incompetent when tasked with correctly assessing a fiber-optic cable. Again, in an industrial environment, especially during installation, it is very important to get this right. I’ve seen too many problems with unstable communications because of cables that were not properly installed or tested from the day they were pulled. Spending money on this will yield big dividends in reliability.

You’ll also need a decent portable scope for working various network and Variable Frequency Drive systems. Some scopes are useful for analyzing bus-oriented networking cables, such as ProfiBus or RS485 multi-drop systems.

If you’re doing any work with wireless, you should get some RF test equipment, and an assortment of attenuators (including some that can handle at least 1 watt or more). If there are licensed radio systems involved you’ll need a service monitor, perhaps a spectrum analyzer, a watt meter, a dummy load, and a calibrated directional coupler. An assortment of hardline coaxial adaptors or waveguide adaptors is essential. A software defined radio can do some of this and there are inexpensive SDR kits, such as the Airspy R2 and the HackRF which offer impressive performance, given what they are.

You may also need an environmental test chamber. It doesn’t have to be fancy. I have used a freezer where you can monitor electrical equipment in cold soak conditions of at least -20 C and heat to at least 60 C. Most industrial electronics work pretty well across this range. However, calibration may drift a bit, and that is rather important to verify and validate.

You may also want to check calibrations of RTDs or Thermistors using boiling liquid nitrogen, water at freezing and boiling water as references. In theory you shouldn’t have to do this for thermocouples, but it’s a useful exercise to ensure that your cold junction compensation system and thermowell is working as it supposed to.

## Side Benefits

So a lab is an interesting place. It’s a significant investment. You may be wondering if it’s worth doing. Well, it is. It all has to do with the care, empowerment, and feeding the needs of Engineers.

Here are few side benefits:

1. Toy Factor for Retention
2. Recruitment
3. Publicity
4. Keep training fresher
5. Improved familiarity with client and vendor equipment
6. Improved overall competence
7. Identification of those who need more training

First, Engineers are kids at heart. Give them toys and they’ll play with them. Those toys could include new products to test, broken equipment to examine, or a safe place to try new ways of dealing with old problems. A steady stream of new ideas and new opportunities to apply new stuff is a great retention tool. It keeps the Engineers engaged and learning. They’ll appreciate it and they’ll even hang around after work and goof around.

Second, Playing with new toys inevitably leads to new blogs and more publicity. If people find out this is a cool place to work, they’ll be eager to come on board. This reduced recruiting and retention costs a lot.

Third, training is expensive. The stuff learned in the training has a surprisingly short half life. It will be nearly unless there is a place to try out that newly learned stuff. A lab can give you a safe place to train in and get fully familiar with the products before deployment.

Fourth, well trained people don’t just emerge out of the ground. They need to practice and play with the stuff. Get good at knowing not just what the gear is supposed to do, but how it does what it does. For example, there are three commands one might use on a Cisco 3850 series switch to enable or disable duplicate IP address detection. Which one is the one that is used on your plant or your specific instance? Would you rather have someone fumbling around in the field while equipment is idle and operators are on overtime? Or would you rather see someone figure it out in the shop and have the exact commands and know for certain that it will work.

And finally, given a context like this, managers can determine who is on the ball and knows their stuff and who may need more training without this discovery happening on a production site.

## Summary

Not all of these features are things you’ll need to do, and not all of them are worth the expense. But the need for a lab is there. A lab isn’t cheap. However, if there is no lab, things will rapidly get a lot more expensive.
