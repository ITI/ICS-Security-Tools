.PHONY: test
PYTHON=python

tests:
	@echo "[ run unit tests ]"
	PYTHONPATH=$(PWD) $(PYTHON) test/test_IPy.py || exit $$?
	@echo
	@echo "[ test README ]"
	$(PYTHON) test_doc.py || exit $$?

egg: clean
	$(PYTHON) setup.py sdist bdist_egg

IPy.html: README
	rst2html README $@ --stylesheet=rest.css

install:
	./setup.py install

clean:
	rm -rf README IPy.html *.pyc build dist IPy.egg-info

