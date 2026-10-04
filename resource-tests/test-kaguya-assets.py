from pathlib import Path
import hashlib,json,struct,zlib
root=Path(__file__).resolve().parents[1]/'mods/kaguya'
manifest=json.loads((root/'manifest.json').read_text())
assert len(manifest['assets'])==25
for asset in manifest['assets']:
    data=(root/asset['path']).read_bytes()
    assert hashlib.sha256(data).hexdigest()==asset['sha256'], asset['path']
    assert len(data)<=256*1024
    if asset['kind']=='portrait':
        assert data[:8]==b'\x89PNG\r\n\x1a\n'
        assert list(struct.unpack('>II',data[16:24]))==asset['size']
        # Full PNG chunk integrity, including IEND; no unverified EXE content.
        offset=8
        while offset<len(data):
            length=struct.unpack('>I',data[offset:offset+4])[0]
            chunk=data[offset+4:offset+8+length];crc=struct.unpack('>I',data[offset+8+length:offset+12+length])[0]
            assert zlib.crc32(chunk)&0xffffffff==crc
            offset+=12+length
        assert offset==len(data) and chunk==b'IEND'
    else:
        assert data[:3] in (b'CWS',b'FWS')
        body=zlib.decompress(data[8:]) if data[:3]==b'CWS' else data[8:]
        assert len(body)+8==struct.unpack('<I',data[4:8])[0]
        offset=(5+4*(body[0]>>3)+7)//8+4
        sounds=[];classes=[];tags=[]
        while offset<len(body):
            tag=struct.unpack_from('<H',body,offset)[0];offset+=2;kind=tag>>6;length=tag&63
            if length==63:length=struct.unpack_from('<I',body,offset)[0];offset+=4
            payload=body[offset:offset+length];assert len(payload)==length;offset+=length;tags.append(kind)
            if kind==14:sounds.append(struct.unpack_from('<H',payload)[0])
            if kind==76:
                count=struct.unpack_from('<H',payload)[0];position=2
                for _ in range(count):
                    char=struct.unpack_from('<H',payload,position)[0];position+=2;end=payload.index(b'\x00',position)
                    classes.append((char,payload[position:end].decode('utf-8')));position=end+1
        assert sounds==[1] and (1,'s1') in classes
        assert set(tags)<={0,1,14,69,76,82},(asset['path'],tags)
assert not list(root.rglob('*.exe')) and not list(root.rglob('*.dll'))
print('PASS: 25 Kaguya asset hashes, PNG dimensions/chunk CRCs and sound SWF length/tags/export s1; Windows binaries excluded.')
