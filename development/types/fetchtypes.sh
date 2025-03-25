while IFS= read -r id; do
    echo "fetching $id"
    wget --content-disposition  --no-check-certificate \
	 --auth-no-challenge\
	 --user="${RAMADDA_USER}" --password="${RAMADDA_PASSWORD}" \
	 "https://localhost:8430/repository/entry/show?entryid=${id}&output=repository.createtype&create=true&applymetadata=true&xauth.user=${RAMADDA_USER}&xauth.password=p"
done < "$1"

