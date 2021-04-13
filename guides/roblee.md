# A Collection of Resources for Getting Started in ICS SCADA Cybersecurity – Robert M. Lee

[Source](http://www.robertmlee.org/a-collection-of-resources-for-getting-started-in-icsscada-cybersecurity/)

"Permalink to A Collection of Resources for Getting Started in ICS/SCADA Cybersecurity – Robert M. Lee"

I commonly get asked by folks what approach they should take to get started in industrial control system (ICS) cybersecurity. Sometimes these individuals have backgrounds in control systems, sometimes they have backgrounds in security, and sometimes they are completely new to both. I have made this blog for the purpose of documenting my thoughts on some good resources out there to pass off to people interested; I will add to it over time as I find other resources I like. Do not attempt to do everything at once but it's a good collection to refer back to in an effort to polish up skills or learn a new industry. Rest assured, no matter how ill prepared you might feel in getting started realize that by having the passion to ask the question and start down the path you are already steps ahead of most. We need passionate people in the industry; everything else can be taught.

## Optional Pre-Reqs

It's always good to pick up a few skills regarding the fundamentals of computers, networks, and systems in general. I would recommend trying to pick up a scripting language as well; even if you don't find yourself scripting a lot understanding how scripting works will add a lot of value to your skill set.

- [Learn Python the Hard Way][1]
  - Learn Python the Hard Way is a great free online resource to teach you, step-by-step, the Python scripting languages. There's a lot of different opinions about different scripting language. In truth, most of them have value in different situations so I'll leave it to you to pick your own language (and I won't tell you that you're wrong for not learning Python, even though you are). Another good programming resource is [Code Academy][2].
- [MIT Introduction to Computer Programming][3]
  - MIT's open courseware is a treasure for the community. It shocks me how many people do not take advantage of free college classes from top universities. This is the Introduction to Computer Science and Programming course. It should be taken at a slow pace but it'll give you a lot of fundamental skills.
- [MIT Introduction to Electrical Engineering and Computer Science][4]
  - Another MIT open course but this time focused on electrical engineering. This is a skill that will help you understand numerous types of control systems better as well as have a better grasp on how computers work.
- [Microsoft Virtual Academy][5]
  - Microsoft Virtual Academy can be found at various locations on YouTube. I have linked to the first one; I would recommend browsing through the topic list for everything from fundamentals of networking, to fundamentals of computers, to how the Internet works.

## Intro to Control Systems

Control systems run the world around us. Escalators, elevators, types of medical equipment, steering in our cars, and building automation systems are types of control systems you interact with daily. Industrial control systems (ICS) are industrial versions of control systems found in locations such as oil drilling, gas pipelines, power grids, water utilities, petrochemical facilities, and more. This section will go over some useful resources and videos to learn more about industrial control systems.

- [The PLC Professor][6]
  - PLC Professor and his website plcprofessor.com contains a lot of great resources for learning what programmable logic controllers (PLCs) and other types of control systems and their logic are and how they work. Some resources are free while others are paid. At some point, getting a physical kit as a trainer to learn on is going to be a requirement.
- [Control System Basics][7]
  - This is a great video explaining control system basics including the type of logic these systems use to sense and create physical changes to take action upon.
- [What is SCADA?][8]
  - You've no doubt heard the term SCADA, if you haven't you will. It stands for Supervisory Control and Data Acquisition and is a type of ICS. This video is a nice basic approach to explaining SCADA.
- [Department of Energy – Energy 101][9]
  - The Department of Energy has a series of Energy 101 videos to explain basic concepts of different types of energy generation, sources, etc. It's a fantastic series that should excite you about the field while explaining key terms and concepts.
- [Wastewater Treatment Explanation Video][10]
  - We all need wastewater treatment facilities and learning about them helps you understand how control systems work and just how complex simple tasks in life can be (if we didn't have control systems). These types of videos are important for you to watch and learn so that you get exposed to different industries. ICS is not really a community, it's a collection of communities.
- [Waste Water – Flush to Finish][11]
  - Another good wastewater explanation video.
- [Refinery Crude Oil Process][12]
  - This is a video explaining a refinery crude oil process. If these types of videos don't excite you to some extent you may be in the wrong career field. The world around us is magnificent and learning different industries will start to help you ask the right questions which will lead to your education on the subject.
- [Natural Gas Processing][13]
  - This is an older video (the industry has definitely become more advanced than represented here) but extremely interesting on how natural gas is harvested, processed, and transferred. Think about all the control systems that have to go into this seeminly simple process.
- [How a Compressor Station Works][14]
  - One particularly interesting (and historically difficult to secure) portion of the ICS community is the natural gas pipeline. This video talks about natural gas to some extent but really focused on compressor stations. Compressor stations as remote sites offer numerous opportunities and challenges to defenders. In short – they're pretty cool.
- [Chemical Engineering YouTube Channel][15]
  - A great series of videos explaining and showing different components of chemical processing.
- [Steel from Start to Finish][16]
  - This is an example of how steel is made. The video, like the others in this section shows an important process that can help you understand all that goes into control system security. It's important to know the real world impacts and applications of the processes we are trying to defend to fully understand how important safety and reliability are as the main component of industrial automation.
- [Nuclear Reactor Explained][17]
  - This is a simplistic but extremely easy to digest explanation and animation of a nuclear reactor. Nuclear energy has a bad rap due to pop culture but is a highly clean and safe form of energy. It's really useful to understand this process and how these systems are designed and, idealy, isolated.
- [Nuclear Power Station][18]
  - Building from the last video, here's another video diving deeper into nuclear power. What you should focus on here is the design and engineering that go into the safety systems. Safety systems can be bypassed, there are no 'unhackable' things, but this helps you to understand just how these systems are designed to be safe by default even if not build with security in mind. The Fukushima event can be observed as a worst case and extremely unlikely scenario. Learning from it will be important; [here ][19]you'll find a good video on it.
- [Thermal Power Plant][20]
  - There are many ways to generate power; this video explains thermal power and the complexity of the environment.
- [SCADA Utility 101][21]
  - Rusty Williams has just the right type of southern speaking which makes an audience want to learn more. The guy is awesome, the video explains SCADA from an electric utility perspective, and this is a much watch.
- [Electric Generation and Transmission][22]
  - Didn't get enough of Rusty? Here's another video of him explaining the generation and transmission of electricity.
- [Control Lectures][23]
  - This is a fantastic series by Brian Douglas which covers a wide range of lectures on control systems in a very easy to process way.
- [Safety Systems][24]
  - It's good to get familiar with safety systems as well. Safety systems can either be active or passive. As an over simplification think of these as systems that take control of the system when an unsafe event occurs and helps to regulate it or shut it down safely. It can also be the product of good engineering instead of a dedicated system. Either way, there is a trend in the community to have integrated safety systems into one device; where the control device is also the safety device. This has cost savings but horrendous cyber security consequences and thus horrible safety consequences.
- [Safety Valves][25]
  - Building on your understanding now of safety systems here's an example of a safety valve in a process and how it can work to keep the operations, and more importantly the people around it, safe.
- [Industrial Disaster Explanation Videos][26]
  - The U.S. Chemical Safety and Hazard Investigation Board has a number of videos explaining industrial disasters. This is an important resource to understand what can go wrong in industrial automation regardless of the cause (these are not cyber related but are important to understand as things that cyber could potentially cause if we are not careful). In IT, if things go wrong people do not generally die – in ICS death, injury, and environmental harm is a very real concern.

## Intro to Computer and Network Security

There's a lot of resources in the form of papers below (especially the SANS Reading Room) which are all great. However, you really need to get hands on so many of the resources are focused on tools and data sets. Try to read up as much as possible and then deeply dive into hands on learning.

- [The Sliding Scale of Cyber Security][27]
  - I wrote this paper specifically to address the nebulous nature of "cyber security." When people say they specialize in cyber security, what exactly does that mean? I put forth that there are 5 categories of investment that can be made. The prioritization for the value towards security should be towards the left hand side of the scale. It is ok to invest in multiple categories at once but understand the true return on investment you're getting versus the cost.
- [VMWare][28]
  - You'll want to be able to set up Virtual machines (VMs) to get hands on with files and various security tools. VMWare is a great choice as is VirtualBox. VMWare has a free version you'll want to use (Player). Don't worry about getting Workstation or Player Pro until later when you are more experienced and want to save snapshots (copies of your VM to revert back to). Below you'll find a sample video on VMs, feel free to Google around for better understanding.
- [Security Onion][29]
  - You're going to want to get hands on with the files presented in this guide; Security Onion is an amazing collection of free tools to do just that with a focus on network security monitoring and traffic analysis.
- [SANS' SIFT][30]
  - If you're super cool you'll want to get into forensics at some point; the SIFT VM from SANS is a collection of tools you'll need to get started.
- [REMnux][31]
  - Before you try out reverse engineering malware (REM) you'll want to have a safe working environment to do so. This is not a beginner topic but at some point you'll likely want to examine malware, Lenny's REMnux VM is the safe place to do that.
- [Malware Traffic Analysis][32]
  - Brad's blog on malware traffic analysis is one of the best resources in the community. It combines sample files with his walk throughs of what they are and how to deal with them. You can learn a lot this way very quickly.
- [Open Security Training][33]
  - This website is dedicated to open (free) security training. There are a number of qualified professionals who have dedicated time to teach things from the basics of security to advanced reverse engineering concept. You could spend quite a time on this website's courses and all of them would make you more capable in this field. There are often full virtual machines (VMs), slides, and videos for the courses.
- [Sample PCAPs from NETRESEC][34]
  - These packet capture samples are invaluable to learning how our systems interact on the network. Take a tool like Wireshark and analyze these files to get familiar with them and the practice (Wireshark will continually be your friend in any field you specialize in).
- [DEFCON Capture the Flag Files][35]
  - DEFCON has made available their files (and often times walkthroughs) for their capture the flag contests. These range from beginner to advanced concepts in offensive security practices such as red teaming. Learning how to break into systems and how they fail is great for defense. It's not required but it can be helpful.
- [Iron Geek][36]
  - This is an invaluable collection of videos from conferences around the community. If you're looking for a specific topic it's a good idea to search these conference videos. Felt like you missed out on the last decade of security? Don't worry most of it's captured here.
- [SANS Reading Room][37]
  - The SANS Institute is the largest and most trusted source of cyber security training. Their Reading Room is a free collection of papers written by students and instructors covering almost every topic in security.
- [Krebs on Security][38]
  - Krebs puts together a great blog doing quality investigative research on breaches, incidents, and cyber security topics that are newsworthy. While doing your self-education keep an eye out for breaking and exciting stories.
- [Honeynet Project][39]
  - Consider this a capstone exercise. Read up on honeypots and learn to deploy a honeypot such as Conpot. The idea is that to run a honeypot correctly you'll have to learn about safeguarding your own infrastructure, setting up proxies and secure tunnels, managing cloud based infrastructure such as an EC2 server, performing traffic analysis on activity in the honeypot, malware analysis on discovered capabilities, and eventually incident response and digital forensics off of the data provided to explore the impact to the system. Working up to this point and then running a successful honeypot for any decent length of time really helps develop and test out a wide range of skills in the Architecture, Passive Defense, Active Defense, and (potentially in the form of Threat Intel) Intelligence categories of the Sliding Scale of Cyber Security.

## Intro to Control System Cyber Security

Cybersecurity is not a new topic but in ICS it is mostly unexplored. The hardest part for most folks is learning who to listen to and what resources to read. There are a lot of "experts" out there who will quickly lead you astray; look at people's resumes to see if they had the opportunity to do what they are speaking to you about. Because they don't have experience doesn't mean they are necessarily wrong but it's an easy check. As an example, if someone calls themselves a "SCADA Security Guru" or something like a "thought leader" but they've only ever been a Chief Marketing Officer of an IT company, that should be a red flag. It is important to be very critical of information in this space but continually push forward to try to make the community better. Below are some trusted resources to help you on your journey.

- [An Abbreviated History of Automation and ICS Cybersecurity][40]
  - This is a great SANS paper looking at the background on ICS cybersecurity. Well worth the read to make sure you understand many of the events that have occurred over the past twenty years and how they've inspired security in ICS today.
- [SANS ICS Library][41]
  - This is the SANS ICS library which contains a number of posters and papers to get you started. Reference the blog as well for good explorations of topics. I write the Defense Use Case series as well which explores real and hyped up ICS attacks and lessons learned from them.
- [SCADAHacker Library][42]
  - Joel has a fantastic collection of papers on ICS security, standards, protocols, systems, etc. Lots of valuable content in this collection.
- [The ICS Cyber Kill Chain][43]
  - The attacks we are concerned most with on ICS take a different approach than traditional IT. This is a paper I wrote with Michael Assante exploring this and detailing the steps an adversary needs to take to accomplish their goals.
- [Analyzing Stuxnet (Windows Portion)][44]
  - This is Bruce Dang's talk at the 27th CCC in Germany on his exploration of analyzing Stuxnet. He was at Microsoft and was one of the first researchers to analyze it. This is a good understanding of the Windows portion of analysis. I show this video even though it's a bit more advanced to highlight that there are often an IT and (operations technology) OT side of analysis.
- [Analyzing Stuxnet (ICS Portion)][45]
  - Ralph Langer was responsible for deep diving into Stuxnet on it's ICS payload portion. This talk gives a good understanding of the OT side of the analysis.
- [To Kill a Centrifuge – Stuxnet Analysis][46]
  - This is Ralph Langer's excellent paper exploring the technical details on the Stuxnet malware and most importantly the ICS specific payload and impact. It is a good idea to read through the paper and Google the terms in the paper you do not understand.
- [SANS ICS Defense Use Case #5 – Ukraine Power Grid Attack][47]
  - This is a paper I wrote with Michael Assante, and Tim Conway released through the E-ISAC on our analysis of the Ukraine power grid attack in 2015. There are also recommendations for defense at each level of the ICS kill chain (applying 1 control is never enough to stop attacks).
- [Perfect ICS Storm][48]
  - Glenn wrote a great paper looking at the interconnectivity of ICS and the networks around them with considerations on how it impacts monitoring and viewing the control systems.
- [Network Security Monitoring in ICS 101][49]
  - Here is a great intro talk on network security monitoring in an ICS by Chris Sistrunk at DEFCON 23. Network security monitoring is exceptionally useful in ICS because it can be done with minimal data sets and passively which works inside the confines of the safety and reliability requirements of an ICS network.
- [Achieving Network Security Monitoring Visibility with Flow Data][50]
  - A SANS webcast with myself and Chris Sander exploring ICS network security monitoring and showing off his tool FlowBAT.
- [S4 Videos][51]
  - The S4 conference run by Dale Peterson is a great community resource. He has posted a number of the conference presentations which will give you a great look at the ICS security community especially from the researcher perspective.
- [Defense Will Win][52]
  - Dale Peterson's excellent S4 talk that has an upbeat attitude of "defense will win." This is something I completely agree with and for a few years now I have been championing the phrase "Defense is Doable" to help folks not get down when it comes to ICS cyber security. It may seem like the hardest challenge out there but it's worthwhile and these are the most defensible environments on the planet; maybe not the most defended – but we will get there.
- [The ICS Cyber Security Challenge][53]
  - This is an annual challenge I put on sponsored by SANS which gives you access to questions and data sets for helping you progress your ICS cyber security skill set.

## Recommended ICS Cybersecurity Books

- [Rise of the Machines: A Cybernetic History][54]
  - It seems a bit odd to put a non-technical book as my first recommendation but I assure you it is with reason. Dr. Thomas Rid wrote this book to attempt to fully understand the history, implications, and usages of the word "cyber". Delightfully, control systems have a major role throughout the book. It was control systems that got us started with "cybernetics" which is eventually where we would have the "cyber" word that fills our daily lives.
- [Handbook of SCADA/Control Systems Security][55]
  - Robert (Bob) Radvanovsky and Jacob (Jake) Brodsky put together this wonderful collection of articles from people throughout the community. It covers a wide variety of topics from a wide variety of personalities and professionals.
- [Protecting Industrial Control Systems from Electronic Threats][56]
  - Joe Weiss is a polarizing individual in the community but only because of how passionately he cares about the industry and how long he's been in the community. Many of us here today in the community owe much to Joe. The scars he carries are from forging a path that has made ICS security much more mainstream.
- [Industrial Network Security][57]
  - Eric Knapp and Joel Langill wrote this book looking specifically at the network security side of ICS. It's a fantastic resource exploring different technologies and protocols by two professionals I'm glad to call peers and friends.
- [Hacking Exposed: Industrial Control Systems][58]
  - This book takes a penetration testing focus on ICS and talks about how to test and assess these systems from the cybersecurity angle while doing it safely and within bounds of acceptable use inside of an ICS. It's written by Clint Bodungen, Bryan Singer, Aaron Shbeeb, Kyle Wilhoit, and Stephen Hilt who all are trusted professionals in the industry.

## Recommended Professional Training

You in no way need certifications or professional training to become great in this field. However, sometimes both can help either for job opportunities, getting a raise, or polishing up some skills you've developed. I highly encourage you to learn as much as you can before getting into a professional class (the more you know going in the more you'll take away) and I encourage you to try to find an employer to pay your way (they aren't cheap). If your employer doesn't have a training policy it's a good time to try and find a new employer. Here are two professional classes I like for ICS cyber security training (I'm biased because I teach at SANS but I teach there because I believe in what they provide).

- [SANS ICS 410 – ICS/SCADA Essentials][59]
  - This class is designed to be a bridge course; if you are an ICS person who wants to learn security, or a security person who wants to learn ICS, this course offers the bridge between those two career fields and offers you an introduction into ICS cyber security.
- [SANS ICS 515 – ICS/SCADA Active Defense and Incident Response][60]
  - This is the class I authored at SANS teaching folks about targeted threats (such as nation-state adversaries or well funded crime groups) that impact ICS and how to hunt them in your environment and respond to incidents.
- [CYBATI ][61]
  - Matt Luallen runs the CYBATI class. It's a hands on class that's been tried and tested and is popular around the community. He sometimes teaches it at SANS events and also teaches at other events. Matt was one of the first people I met in the ICS security community and has been like a brother to me over the years; he's a fantastic resource for the community and more importantly he's just a really good person. Learning from him (and getting to use his CYBATIworks kit which is a really useful training kit for sale) is something everyone should get to do at some point in their career.

## Recommended Conferences

No matter how much time you spend reading or practicing eventually you need to become part of the community. Contributions in the form of research, writing, and tools are always appreciated. Contributions in the form of conference presentations are especially helpful as they introduce you to other interested folks. The ICS cybersecurity community is an important one on many levels. It's one of the best communities out there with hard working and passionate people who care about making the world a safer place. Below are what I consider the big 5. These conferences are the ones that are general ICS cyber security (not a specific industry such as API for oil and gas or GridSecCon for electric sector) although those are valuable as well.

- [SANS ICS Security Summit][62]
  - For over a decade the SANS ICS Security Summit has been a leading conference on bringing together researchers, industry professionals, and government audiences. The page above links to the various SANS ICS events but look for the one that says "ICS Security Summit" each year. It is usually held in March at Disney World in Orlando Florida. It's strong suit is the educational and training aspects not only because of the classes but also because of the strong industry focus.
- [DigitalBond's S4][63]
  - The S4 conference is a powerhouse of leading ICS security research. Dale puts on a fantastic conference every year (now with a European and Japanese venue as well each year) that brings together some of the most cutting edge research and ideas. S4 in the US is often held in January in Florida.
- [The ICS Cyber Security Conference (WeissCon)][64]
  - Affectionately known as WeissCon after it's founder Joe Weiss, the conference is now owned and operated by SecurityWeek and usually runs in October at different locations each year in the US (Georgia is usually a central location for the conference though). The conference brings together a portion of the community not often found at the other locations and has a strong buy-in from the government community as well as the vendor community.
- [The ICS Joint Working Group (ICSJWG)][65]
  - The ICSJWG is a free conference held twice a year by the Department of Homeland Security. I often encourage people to go to the ICSJWG conference first as a type of intro into the community, to then go to the SANS ICS Security Summit for more view into the asset owner community and to get training, then go to S4 for the latest research, to go to WeissCon to see some of the portions of the community and vendor audience not represented elsewhere, and finally to 4SICS to get an international view. It is perfectly ok to go to all five of the big conferences a year (I do) but if you need a general path that is the one I would follow initially.
- [4SICS][66]
  - The 4SICS is held every year in Stockholm, Sweden usually in October and is a fantastic collection of ICS professionals from around Europe. The conference usually attracts the same type of research and big named audience that you would find at S4 but with deep roots in Europe as represented by its founders Erik and Robert. They are two of the friendliest people in the ICS community and have a wealth of experience from decades of experience defending infrastructure. Stockholm is cold in the winter but the people and their optimism will keep you warm.

This is just a small collection of a lot of the fantastic resources out there. I will continually try to update it as especially good materials are made available. Always fight to be part of the community and interact – that is where the real value in learning is. Never wait to have someone show you though, even the "experts" are usually only expert in a few things. It is up to you to teach yourself and involve yourself. We as a community are waiting open armed.

[1]: http://learnpythonthehardway.org/
[2]: https://www.codecademy.com/
[3]: http://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-00-introduction-to-computer-science-and-programming-fall-2008/
[4]: http://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-01sc-introduction-to-electrical-engineering-and-computer-science-i-spring-2011/
[5]: https://www.youtube.com/watch?v=t9TmvFvYfWw
[6]: https://www.youtube.com/user/plcprofessor
[7]: https://www.youtube.com/watch?v=VQLRVjEFRGI
[8]: https://www.youtube.com/watch?v=bfxr5DikdP0
[9]: https://www.youtube.com/watch?v=l4wldZsR7OY&list=PLACD8E92715335CB2
[10]: https://www.youtube.com/watch?v=ZSFdOjxB-1I
[11]: https://www.youtube.com/watch?v=pRaptzcp9G4
[12]: https://www.youtube.com/watch?v=gYnGgre83CI
[13]: https://www.youtube.com/watch?v=4DG2gW2D-cU
[14]: https://www.youtube.com/watch?v=-nOhsyuIV3o
[15]: https://www.youtube.com/user/pratheepthavara
[16]: https://www.youtube.com/watch?v=9l7JqonyoKA
[17]: https://www.youtube.com/watch?v=1U6Nzcv9Vws
[18]: https://www.youtube.com/watch?v=vggzl9OngaM
[19]: https://www.youtube.com/watch?v=JMaEjEWL6PU
[20]: https://www.youtube.com/watch?v=IdPTuwKEfmA
[21]: https://www.youtube.com/watch?v=vv2CoTiaWPI
[22]: https://www.youtube.com/watch?v=nJ-eBqEnraE
[23]: https://www.youtube.com/user/ControlLectures
[24]: http://www.pacontrol.com/safetysystems.html
[25]: https://www.youtube.com/watch?v=0aYzgj_AXxc
[26]: https://www.youtube.com/channel/UCXIkr0SRTnZO4_QpZozvCCA
[27]: https://www.sans.org/reading-room/whitepapers/analyst/sliding-scale-cyber-security-36240
[28]: http://www.vmware.com/products/player.html
[29]: https://github.com/Security-Onion-Solutions/security-onion/wiki/Installation
[30]: http://digital-forensics.sans.org/community/downloads
[31]: https://remnux.org/docs/distro/get/
[32]: http://www.malware-traffic-analysis.net/index.html
[33]: http://opensecuritytraining.info/Training.html
[34]: http://www.netresec.com/?page=PcapFiles
[35]: https://www.defcon.org/html/links/dc-ctf.html
[36]: http://www.irongeek.com/
[37]: https://www.sans.org/reading-room
[38]: http://krebsonsecurity.com/
[39]: https://www.honeynet.org/
[40]: https://ics.sans.org/media/An-Abbreviated-History-of-Automation-and-ICS-Cybersecurity.pdf
[41]: https://ics.sans.org/ics-library
[42]: https://scadahacker.com/library
[43]: https://www.sans.org/reading-room/whitepapers/ICS/industrial-control-system-cyber-kill-chain-36297
[44]: https://www.youtube.com/watch?v=rOwMW6agpTI
[45]: https://www.youtube.com/watch?v=zBjmm48zwQU
[46]: http://www.langner.com/en/wp-content/uploads/2013/11/To-kill-a-centrifuge.pdf
[47]: http://ics.sans.org/duc5
[48]: https://www.giac.org/paper/gcia/10551/perfect-ics-storm/141222
[49]: https://www.youtube.com/watch?v=H6AWRziR028
[50]: https://www.sans.org/webcasts/achieving-ics-network-security-monitoring-visibility-flow-data-100172
[51]: https://www.youtube.com/channel/UC5MdLu7ji_eyGiTfigk75lQ
[52]: https://www.youtube.com/watch?v=t5F93NIDePQ
[53]: https://icscybersecuritychallenge.com/
[54]: https://www.amazon.com/Rise-Machines-Cybernetic-Thomas-Rid/dp/0393286002/
[55]: https://www.amazon.com/Handbook-Control-Systems-Security-Second/dp/1498717071/ref=sr_1_5?s=books&ie=UTF8&qid=1472416488&sr=1-5&keywords=scada+security
[56]: https://www.amazon.com/Protecting-Industrial-Control-Systems-Electronic/dp/1606501976/ref=sr_1_1?s=books&ie=UTF8&qid=1472416648&sr=1-1&keywords=joe+weiss
[57]: https://www.amazon.com/Industrial-Network-Security-Second-Infrastructure/dp/0124201148/ref=sr_1_1?s=books&ie=UTF8&qid=1472416488&sr=1-1&keywords=scada+security
[58]: https://www.amazon.com/Hacking-Exposed-Industrial-Control-Systems/dp/1259589714/
[59]: http://sans.org/ics410
[60]: http://sans.org/ics515
[61]: https://cybati.org/
[62]: http://ics.sans.org/events
[63]: http://www.digitalbond.com/s4/
[64]: http://www.icscybersecurityconference.com/
[65]: https://ics-cert.us-cert.gov/Industrial-Control-Systems-Joint-Working-Group-ICSJWG
[66]: https://4sics.se/
