
function RamaddaMakeAlias(nameId,aliasId) {
    let nameInput = jqid(nameId);
    let cbx=jqid(aliasId);
    let aliasInput = jqid(aliasId+"_input");
    let applyAlias = () =>{
	let name  = nameInput.val()
	let alias = Utils.makeID(name);
	aliasInput.val(alias);
    }
    cbx.change(function(){
	applyAlias();
    });
    nameInput.keyup((e)=> {
	if(HU.isChecked(cbx)) {
	    applyAlias();
	}
    });
}
